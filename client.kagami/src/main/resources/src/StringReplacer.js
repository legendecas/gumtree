export default class StringReplacer {
  constructor (original) {
    this.orig = original
    this.actions = []
    this.result = original
  }

  replace (start, length, padStart = '', padEnd = '') {
    let newStart = start
    let newLength = length
    this.actions.forEach(it => {
      const { start: ostStart, length: ostLength, padding: ostPadding, offset } = it
      switch (true) {
        case ostStart + ostLength <= start:
          newStart += offset
          break
        case ostStart <= start && start + length <= ostStart + ostLength:
          newStart += ostPadding
          break
        case start <= ostStart && ostStart + ostLength < start + length:
          newLength += offset
          break
      }
    })
    this.actions.push({
      start,
      length,
      padding: padStart.length,
      offset: padStart.length + padEnd.length
    })

    const temp = [...this.result]
    const wrap = temp.slice(newStart, newStart + newLength).join('')
    temp.splice(newStart, newLength, ...padStart, ...wrap, ...padEnd)
    this.result = temp.join('')
  }

  toString () {
    return this.result
  }
}

// window.StringReplacer = StringReplacer
// const s = new StringReplacer('const a = a + 1')
// s.replace(6, 1, '<1>', '</1>')
// console.log(s.toString())
// s.replace(10, 5, '<2>', '</2>')
// console.log(s.toString())
// s.replace(10, 1, '<3>', '</3>')
// console.log(s.toString())
// s.replace(7, 8, '<4>', '</4>')
// console.log(s.toString())
