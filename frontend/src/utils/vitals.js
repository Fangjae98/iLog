// @metric LCP, FCP, TTFB, INP, CLS: 개발 중 Core Web Vitals 값을 콘솔에서 바로 확인한다
export async function reportWebVitals() {
  const { onLCP, onFCP, onTTFB, onINP, onCLS } = await import('web-vitals')

  const log = ({ name, value, rating }) => {
    const shown = name === 'CLS' ? value.toFixed(3) : `${Math.round(value)}ms`
    console.info(`[web-vitals] ${name} ${shown} (${rating})`)
  }

  onLCP(log)
  onFCP(log)
  onTTFB(log)
  onINP(log) // 화면을 조작한 뒤 탭을 숨기거나 떠날 때 보고된다
  onCLS(log)
}
