/**
 * Split text into typewriter units without breaking math delimiters or Unicode chars.
 */
export function splitTypewriterUnits(text) {
  const total = String(text || '')
  const units = []
  let index = 0

  while (index < total.length) {
    const formulaEnd = findMathBlockEnd(total, index)
    if (formulaEnd > index) {
      units.push(total.slice(index, formulaEnd))
      index = formulaEnd
      continue
    }

    const code = total.codePointAt(index)
    if (code == null) break
    const size = code > 0xffff ? 2 : 1
    const char = total.slice(index, index + size)

    if (!/[\u4e00-\u9fff\u3000-\u303f\uff00-\uffef]/.test(char) && char !== '\n') {
      let batch = char
      let next = index + size
      while (batch.length < 4 && next < total.length) {
        const nextEnd = findMathBlockEnd(total, next)
        if (nextEnd > next) break
        const nextCode = total.codePointAt(next)
        if (nextCode == null) break
        const nextSize = nextCode > 0xffff ? 2 : 1
        const nextChar = total.slice(next, next + nextSize)
        if (/[\u4e00-\u9fff\u3000-\u303f\uff00-\uffef]/.test(nextChar) || nextChar === '\n' || nextChar === '$') {
          break
        }
        batch += nextChar
        next += nextSize
      }
      units.push(batch)
      index += batch.length
      continue
    }

    if (/[\u4e00-\u9fff]/.test(char)) {
      let batch = char
      let next = index + size
      if (next < total.length) {
        const nextEnd = findMathBlockEnd(total, next)
        if (nextEnd === next) {
          const nextCode = total.codePointAt(next)
          if (nextCode != null) {
            const nextSize = nextCode > 0xffff ? 2 : 1
            const nextChar = total.slice(next, next + nextSize)
            if (/[\u4e00-\u9fff]/.test(nextChar)) {
              batch += nextChar
              next += nextSize
            }
          }
        }
      }
      units.push(batch)
      index += batch.length
      continue
    }

    units.push(char)
    index += size
  }

  return units
}

function findMathBlockStart(text, index) {
  let i = 0
  while (i < index) {
    if (text.startsWith('$$', i)) {
      const close = text.indexOf('$$', i + 2)
      if (close === -1 || close >= index) return i
      i = close + 2
      continue
    }
    if (text[i] === '$') {
      const close = text.indexOf('$', i + 1)
      if (close === -1 || close >= index) return i
      i = close + 1
      continue
    }
    i += 1
  }
  return -1
}

function findMathBlockEnd(text, index) {
  if (text.startsWith('$$', index)) {
    const close = text.indexOf('$$', index + 2)
    return close === -1 ? text.length : close + 2
  }
  if (text[index] === '$') {
    const close = text.indexOf('$', index + 1)
    return close === -1 ? text.length : close + 1
  }
  const open = findMathBlockStart(text, index)
  if (open >= 0) {
    if (text.startsWith('$$', open)) {
      const close = text.indexOf('$$', open + 2)
      return close === -1 ? text.length : close + 2
    }
    const close = text.indexOf('$', open + 1)
    return close === -1 ? text.length : close + 1
  }
  return index
}

/**
 * Run a fast typewriter effect on plain text/markdown source.
 */
export function runTypewriter({ text, onUpdate, onComplete, interval = 18 }) {
  const units = splitTypewriterUnits(text)
  let pos = 0
  let timer = null
  let stopped = false

  const tick = () => {
    if (stopped) return
    if (pos >= units.length) {
      onUpdate(String(text || ''))
      if (onComplete) onComplete()
      return
    }
    pos += 1
    onUpdate(units.slice(0, pos).join(''))
    timer = setTimeout(tick, interval)
  }

  tick()

  return () => {
    stopped = true
    if (timer) clearTimeout(timer)
  }
}
