/**
 * Fly a question card clone into the basket float button.
 */
const FLY_DURATION_MS = 720
const TARGET_SELECTOR = '[data-qb-basket-target]'

function ensureFlyStyles() {
  if (typeof document === 'undefined' || document.getElementById('qb-basket-fly-style')) return
  const style = document.createElement('style')
  style.id = 'qb-basket-fly-style'
  style.textContent = `
[data-qb-basket-target].qb-basket-float--pulse {
  animation: qb-basket-float-pulse 0.42s ease;
}
@keyframes qb-basket-float-pulse {
  0% { transform: translateY(-50%) scale(1); }
  40% { transform: translateY(-50%) scale(1.1); box-shadow: -4px 4px 18px rgba(230, 162, 60, 0.35); }
  100% { transform: translateY(-50%) scale(1); }
}
.qb-basket-fly-clone {
  box-shadow: 0 12px 32px rgba(230, 162, 60, 0.35) !important;
  border: 2px solid #f0c78a !important;
}
`
  document.head.appendChild(style)
}

function getBasketTarget() {
  return document.querySelector(TARGET_SELECTOR)
}

function pulseBasketTarget() {
  const target = getBasketTarget()
  if (!target) return
  target.classList.add('qb-basket-float--pulse')
  window.setTimeout(() => target.classList.remove('qb-basket-float--pulse'), 420)
}

function runFlyAnimation(clone, sourceRect, targetRect) {
  const endX = targetRect.left + targetRect.width / 2
  const endY = targetRect.top + targetRect.height / 2
  const startX = sourceRect.left + sourceRect.width / 2
  const startY = sourceRect.top + sourceRect.height / 2
  const dx = endX - startX
  const dy = endY - startY
  const endScale = Math.min(56 / sourceRect.width, 64 / sourceRect.height, 0.22)

  if (typeof clone.animate === 'function') {
    return clone.animate([
      { transform: 'translate(0, 0) scale(1)', opacity: 1 },
      { transform: `translate(${dx}px, ${dy}px) scale(${endScale})`, opacity: 0.35 }
    ], {
      duration: FLY_DURATION_MS,
      easing: 'cubic-bezier(0.55, 0, 0.75, 0.4)',
      fill: 'forwards'
    }).finished
  }

  return new Promise(resolve => {
    clone.style.transition = `transform ${FLY_DURATION_MS}ms cubic-bezier(0.55, 0, 0.75, 0.4), opacity ${FLY_DURATION_MS}ms ease`
    window.requestAnimationFrame(() => {
      clone.style.transform = `translate(${dx}px, ${dy}px) scale(${endScale})`
      clone.style.opacity = '0.35'
    })
    window.setTimeout(resolve, FLY_DURATION_MS + 30)
  })
}

export function flyToBasket(sourceEl) {
  return new Promise(resolve => {
    ensureFlyStyles()
    if (!sourceEl || typeof document === 'undefined') {
      resolve(false)
      return
    }
    const target = getBasketTarget()
    if (!target) {
      resolve(false)
      return
    }

    const sourceRect = sourceEl.getBoundingClientRect()
    if (!sourceRect.width || !sourceRect.height) {
      resolve(false)
      return
    }
    const targetRect = target.getBoundingClientRect()

    const clone = sourceEl.cloneNode(true)
    clone.classList.add('qb-basket-fly-clone')
    clone.style.position = 'fixed'
    clone.style.left = `${sourceRect.left}px`
    clone.style.top = `${sourceRect.top}px`
    clone.style.width = `${sourceRect.width}px`
    clone.style.height = `${sourceRect.height}px`
    clone.style.zIndex = '4000'
    clone.style.margin = '0'
    clone.style.pointerEvents = 'none'
    clone.style.overflow = 'hidden'
    clone.style.boxSizing = 'border-box'
    clone.style.transformOrigin = 'center center'
    clone.style.willChange = 'transform, opacity'

    sourceEl.classList.add('qb-card-fly-source-hidden')
    document.body.appendChild(clone)

    const finish = () => {
      clone.remove()
      pulseBasketTarget()
      resolve(true)
    }

    runFlyAnimation(clone, sourceRect, targetRect)
      .then(finish)
      .catch(finish)
  })
}

export function markCardFlyingOut(cardEl) {
  if (cardEl) cardEl.classList.add('qb-card-fly-out')
}

export function resetCardFlyingOut(cardEl) {
  if (cardEl) {
    cardEl.classList.remove('qb-card-fly-out')
    cardEl.classList.remove('qb-card-fly-source-hidden')
  }
}

export function getBasketTargetElement() {
  return getBasketTarget()
}
