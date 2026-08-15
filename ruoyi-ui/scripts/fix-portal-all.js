const { execSync } = require('child_process')
const path = require('path')

const dir = __dirname
const scripts = [
  'fix-portal-encoding.js',
  'fix-portal-encoding-part2.js',
  'fix-portal-encoding-part3.js',
  'write-portal-login.js',
  'patch-portal-login.js',
  'dedupe-portal-login-import.js',
  'patch-portal-browse.js',
  'patch-portal-nav-subject.js',
  'apply-portal-theme.js',
  'apply-portal-layout.js',
  'apply-portal-white-blue-theme.js',
  'write-portal-shell.js',
  'write-portal-nav.js',
  'write-portal-home.js',
  'write-portal-browse.js',
  'write-portal-paper.js',
  'write-portal-float-bar.js',
  'write-portal-auth-dialog.js',
  'write-portal-profile.js',
  'write-portal-my-papers.js'
]

for (const s of scripts) {
  console.log('\n>>>', s)
  execSync(`node "${path.join(dir, s)}"`, { stdio: 'inherit' })
}
console.log('\nAll portal UTF-8 fixes applied.')
