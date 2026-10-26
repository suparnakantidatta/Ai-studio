function transpilePython(code) {
  const rawLines = code.split('\n');
  const logicalLines = [];
  let buffer = '';
  let bracketDepth = 0;
  let startIndent = 0;

  for (let raw of rawLines) {
    const trimmed = raw.trim();
    if (!trimmed || trimmed.startsWith('#')) {
      if (bracketDepth === 0) continue;
    }

    let inQuotes = false;
    let quoteChar = '';
    for (let i = 0; i < raw.length; i++) {
      const ch = raw[i];
      if ((ch === '"' || ch === '\'') && (i === 0 || raw[i-1] !== '\\')) {
        if (!inQuotes) { inQuotes = true; quoteChar = ch; }
        else if (quoteChar === ch) { inQuotes = false; }
      } else if (!inQuotes) {
        if (ch === '(' || ch === '[' || ch === '{') bracketDepth++;
        else if (ch === ')' || ch === ']' || ch === '}') bracketDepth = Math.max(0, bracketDepth - 1);
      }
    }

    if (buffer === '') {
      const match = raw.match(/^\s*/);
      startIndent = match ? match[0].replace(/\t/g, '    ').length : 0;
      buffer = trimmed;
    } else {
      buffer += ' ' + trimmed;
    }

    if (bracketDepth === 0) {
      logicalLines.push({ indent: startIndent, text: buffer });
      buffer = '';
    }
  }
  if (buffer) {
    logicalLines.push({ indent: startIndent, text: buffer });
  }

  // Collect all assigned variables
  const allVars = new Set();
  for (let item of logicalLines) {
    const line = item.text;
    const assignMatch = line.match(/^([a-zA-Z_]\w*(?:\s*,\s*[a-zA-Z_]\w*)*)\s*=/);
    if (assignMatch && !line.startsWith('if') && !line.startsWith('for') && !line.startsWith('while')) {
      assignMatch[1].split(',').forEach(v => allVars.add(v.trim()));
    }
    const forMatch = line.match(/^for\s+([a-zA-Z_]\w*(?:\s*,\s*[a-zA-Z_]\w*)*)\s+in/);
    if (forMatch) {
      forMatch[1].split(',').forEach(v => allVars.add(v.trim()));
    }
  }

  const jsLines = [];
  const indentStack = [0];

  if (allVars.size > 0) {
    jsLines.push('var ' + Array.from(allVars).join(', ') + ';');
  }

  for (let item of logicalLines) {
    const indent = item.indent;
    let line = item.text;

    while (indent < indentStack[indentStack.length - 1]) {
      indentStack.pop();
      jsLines.push(' '.repeat(indentStack[indentStack.length - 1]) + '}');
    }

    const hashIdx = line.indexOf(' #');
    if (hashIdx !== -1) line = line.substring(0, hashIdx).trim();

    const endsWithColon = line.endsWith(':');
    if (endsWithColon) line = line.substring(0, line.length - 1).trim();

    line = line
      .replace(/\bTrue\b/g, 'true')
      .replace(/\bFalse\b/g, 'false')
      .replace(/\bNone\b/g, 'null')
      .replace(/\band\b/g, '&&')
      .replace(/\bor\b/g, '||')
      .replace(/\bnot\s+/g, '!')
      .replace(/\.lower\(\)/g, '.toLowerCase()')
      .replace(/\.upper\(\)/g, '.toUpperCase()')
      .replace(/\.strip\(\)/g, '.trim()')
      .replace(/\.append\s*\(/g, '.push(');

    // f-strings
    line = line.replace(/f(["'])(.*?)\1/g, function(m, quote, content) {
      var parts = content.split(/\{([^}]+)\}/);
      var exprs = [];
      for (var p = 0; p < parts.length; p++) {
        if (p % 2 === 0) {
          if (parts[p]) exprs.push(JSON.stringify(parts[p]));
        } else {
          var cleanExpr = parts[p].trim();
          if (cleanExpr.indexOf(':.') !== -1) {
            var fmtParts = cleanExpr.split(':.');
            var numVal = fmtParts[0];
            var digits = parseInt(fmtParts[1]) || 2;
            exprs.push('Number(' + numVal + ').toFixed(' + digits + ')');
          } else {
            exprs.push('(' + cleanExpr + ')');
          }
        }
      }
      return exprs.join(' + ');
    });

    // Slices [::-1]
    line = line.replace(/([a-zA-Z_]\w*)\[::-1\]/g, '(typeof $1 === "string" ? $1.split("").reverse().join("") : [...$1].reverse())');

    // Ternary: a if cond else b -> (cond ? a : b)
    line = line.replace(/([^\s]+)\s+if\s+([^:]+?)\s+else\s+([^\s;]+)/g, '($2 ? $1 : $3)');

    if (line.startsWith('def ')) {
      const rest = line.substring(4).trim();
      const fnName = rest.substring(0, rest.indexOf('(')).trim();
      const args = rest.substring(rest.indexOf('('));
      jsLines.push(' '.repeat(indent) + 'function ' + fnName + args + ' {');
      indentStack.push(indent + 4);
      continue;
    }

    if (line.startsWith('if ')) {
      const cond = line.substring(3).trim();
      jsLines.push(' '.repeat(indent) + 'if (' + cond + ') {');
      indentStack.push(indent + 4);
      continue;
    }

    if (line.startsWith('elif ')) {
      const cond = line.substring(5).trim();
      if (indentStack.length > 1) indentStack.pop();
      jsLines.push(' '.repeat(indent) + '} else if (' + cond + ') {');
      indentStack.push(indent + 4);
      continue;
    }

    if (line === 'else') {
      if (indentStack.length > 1) indentStack.pop();
      jsLines.push(' '.repeat(indent) + '} else {');
      indentStack.push(indent + 4);
      continue;
    }

    if (line.startsWith('while ')) {
      const cond = line.substring(6).trim();
      jsLines.push(' '.repeat(indent) + 'while (' + cond + ') {');
      indentStack.push(indent + 4);
      continue;
    }

    if (line.startsWith('for ')) {
      const forContent = line.substring(4).trim();
      if (forContent.includes(' in ')) {
        const [iterVar, iterObj] = forContent.split(' in ').map(s => s.trim());
        if (iterObj.includes('.items()')) {
          const dictObj = iterObj.replace('.items()', '');
          const vars = iterVar.split(',').map(v => v.trim());
          jsLines.push(' '.repeat(indent) + 'for (let [' + vars.join(', ') + '] of Object.entries(' + dictObj + ')) {');
        } else {
          jsLines.push(' '.repeat(indent) + 'for (let ' + iterVar + ' of ' + iterObj + ') {');
        }
        indentStack.push(indent + 4);
        continue;
      }
    }

    const multiAssign = line.match(/^([a-zA-Z_]\w*(?:\s*,\s*[a-zA-Z_]\w*)+)\s*=\s*(.+)$/);
    if (multiAssign) {
      const vars = multiAssign[1].split(',').map(v => v.trim());
      const exprs = multiAssign[2].split(',').map(e => e.trim());
      jsLines.push(' '.repeat(indent) + '[' + vars.join(', ') + '] = [' + exprs.join(', ') + '];');
      continue;
    }

    jsLines.push(' '.repeat(indent) + line + (line.endsWith(';') ? '' : ';'));
  }

  while (indentStack.length > 1) {
    indentStack.pop();
    jsLines.push('}');
  }

  return jsLines.join('\n');
}

function test(name, code) {
  console.log('=== ' + name + ' ===');
  let output = '';
  function print(...args) {
    output += args.map(a => typeof a === 'object' ? JSON.stringify(a) : String(a)).join(' ') + '\n';
  }
  function range(a, b, step = 1) {
    const arr = [];
    let start = 0, end = a;
    if (b !== undefined) { start = a; end = b; }
    if (step > 0) { for (let i = start; i < end; i += step) arr.push(i); }
    else { for (let i = start; i > end; i += step) arr.push(i); }
    return arr;
  }
  function len(x) { return x ? (x.length !== undefined ? x.length : Object.keys(x).length) : 0; }
  function sum(x) { return Array.isArray(x) ? x.reduce((a, b) => a + Number(b), 0) : 0; }
  function min(...x) { const flat = Array.isArray(x[0]) ? x[0] : x; return Math.min(...flat); }
  function max(...x) { const flat = Array.isArray(x[0]) ? x[0] : x; return Math.max(...flat); }
  function abs(x) { return Math.abs(x); }

  const js = transpilePython(code);
  try {
    const fn = new Function('print', 'range', 'len', 'sum', 'min', 'max', 'abs', js);
    fn(print, range, len, sum, min, max, abs);
    console.log(output);
  } catch(e) {
    console.error('ERROR in ' + name + ':', e.message);
    console.log('Generated JS:\n', js);
  }
}

test('Fibonacci', `
def fibonacci(n):
    a, b = 0, 1
    res = []
    for _ in range(n):
        res.append(a)
        a, b = b, a + b
    return res

print("Fibonacci:", fibonacci(10))
`);

test('Students Dict', `
students = {
    "Rohan": 92,
    "Priya": 85,
    "Amit": 74,
    "Sneha": 98
}

for name, score in students.items():
    grade = "A+" if score >= 90 else "A" if score >= 80 else "B"
    print(f"{name}: {score} -> Grade {grade}")
`);

test('Palindrome', `
words = ["madam", "python", "racecar", "pathsala", "radar"]

for w in words:
    if w == w[::-1]:
        print(f"'{w}' is a Palindrome")
    else:
        print(f"'{w}' is NOT a palindrome")
`);
