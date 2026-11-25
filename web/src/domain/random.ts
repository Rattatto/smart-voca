// 2026-11-25 - Smart Voca - 난수 함수와 섞기. Kotlin Random(seed), shuffled(random) 대체
// 파일 위치: web/src/domain/random.ts

/** 0 이상 1 미만의 수를 돌려주는 함수. 기본은 Math.random, 테스트에서는 시드 고정 함수를 넣는다. */
export type Rng = () => number

/** 시드가 같으면 같은 순서를 내는 작은 난수 생성기 (mulberry32). 테스트 재현용. */
export function seededRng(seed: number): Rng {
  let state = seed >>> 0
  return () => {
    state = (state + 0x6d2b79f5) >>> 0
    let value = state
    value = Math.imul(value ^ (value >>> 15), value | 1)
    value ^= value + Math.imul(value ^ (value >>> 7), value | 61)
    return ((value ^ (value >>> 14)) >>> 0) / 4_294_967_296
  }
}

/** 0 이상 bound 미만 정수. */
export function nextInt(rng: Rng, bound: number): number {
  return Math.floor(rng() * bound)
}

/** 원본을 바꾸지 않고 섞은 새 배열을 돌려준다 (Fisher-Yates). */
export function shuffled<T>(items: readonly T[], rng: Rng = Math.random): T[] {
  const result = [...items]
  for (let index = result.length - 1; index > 0; index--) {
    const swapIndex = nextInt(rng, index + 1)
    ;[result[index], result[swapIndex]] = [result[swapIndex], result[index]]
  }
  return result
}
