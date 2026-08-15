import Vue from 'vue'

export const portalAuthBus = new Vue()

export function openPortalAuth(options = {}) {
  portalAuthBus.$emit('open', options || {})
}
