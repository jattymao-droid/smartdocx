/**
 * Apply school crimson/gold theme to all portal write scripts output.
 * Run: node scripts/apply-portal-school-theme.js
 */
const { execSync } = require('child_process')
const path = require('path')

const dir = __dirname
const scripts = [
  'write-portal-home.js',
  'write-portal-paper.js',
  'write-portal-browse.js',
  'write-portal-float-bar.js',
  'write-portal-nav.js',
  'write-portal-auth-dialog.js',
  'write-portal-shell.js'
]

for (const s of scripts) {
  console.log('>>>', s)
  execSync(`node "${path.join(dir, s)}"`, { stdio: 'inherit' })
}
console.log('School crimson theme applied.')
