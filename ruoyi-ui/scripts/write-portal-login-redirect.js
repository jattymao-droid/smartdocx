/** Redirect /portal/login to home + open auth modal. */
const fs = require('fs')
const path = require('path')
const file = path.join(__dirname, '../src/views/portal/Login.vue')
const content = `<template>
  <div class="portal-login-redirect" />
</template>

<script>
import { openPortalAuth } from '@/utils/portalAuth'

export default {
  name: 'PortalLogin',
  created() {
    const redirect = this.$route.query.redirect
    openPortalAuth({ redirect, tab: 'login' })
    this.$router.replace(redirect || '/portal/home').catch(() => {})
  }
}
</script>
`
fs.writeFileSync(file, content, 'utf8')
console.log('wrote Login.vue redirect stub')
