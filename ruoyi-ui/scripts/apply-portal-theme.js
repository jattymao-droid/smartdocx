/**
 * Apply portal theme. Run: node scripts/apply-portal-theme.js
 */
const fs = require('fs')
const path = require('path')

const src = path.join(__dirname, '../src')

/** Nile Blue palette (??????) ?? RGB(82,170,193) */
const C = {
  primary: '#52aac1',
  primaryLight: '#6ebdd4',
  primarySoft: '#73b290',
  primaryHover: '#3d94ab',
  primaryText: '#3d7a8f',
  tintBg: '#eef6f9',
  tintBorder: '#d4e8ef',
  tintActive: '#dceef5',
  tintHover: '#f0f8fb',
  shadow: 'rgba(82, 170, 193, 0.1)',
  shadowMd: 'rgba(82, 170, 193, 0.16)',
  pageBg: '#f6f9fb',
  heroFrom: '#6ebdd4',
  heroTo: '#52aac1',
  heroText: '#ffffff',
  loginFrom: '#52aac1',
  loginMid: '#4599b0',
  loginTo: '#3d8a9e',
  loginText: '#ffffff',
  accentAmber: '#e8a54b',
  accentMuted: '#b5b1c7',
  accentPeach: '#f6d6b1'
}

function write(rel, content) {
  const f = path.join(src, rel)
  fs.mkdirSync(path.dirname(f), { recursive: true })
  fs.writeFileSync(f, content, 'utf8')
  console.log('wrote', rel)
}

function patchStyle(rel, newStyle) {
  const f = path.join(src, rel)
  let text = fs.readFileSync(f, 'utf8')
  const re = /<style scoped lang="scss">[\s\S]*<\/style>/
  if (!re.test(text)) {
    console.warn('no style block:', rel)
    return
  }
  text = text.replace(re, `<style scoped lang="scss">\n${newStyle}\n</style>`)
  fs.writeFileSync(f, text, 'utf8')
  console.log('styled', rel)
}

function patchTemplate(rel, from, to) {
  const f = path.join(src, rel)
  let text = fs.readFileSync(f, 'utf8')
  if (!text.includes(from)) return
  text = text.split(from).join(to)
  fs.writeFileSync(f, text, 'utf8')
  console.log('patched template', rel)
}

write('assets/styles/portal.scss', `/* Portal design system - Nile Blue */
$portal-primary: ${C.primary};
$portal-primary-light: ${C.primaryLight};
$portal-accent: ${C.accentAmber};
$portal-ink: #1e293b;
$portal-bg: ${C.pageBg};
$portal-border: ${C.tintBorder};
$portal-radius: 12px;
$portal-shadow: 0 4px 24px ${C.shadow};

.portal-page {
  min-height: calc(100vh - 120px);
  background: $portal-bg;
  background-image:
    radial-gradient(ellipse 80% 50% at 50% -20%, rgba(82, 170, 193, 0.12), transparent),
    radial-gradient(ellipse 60% 40% at 100% 0%, rgba(232, 165, 75, 0.05), transparent);
}

.portal-container {
  width: 1280px;
  max-width: 100%;
  margin: 0 auto;
  padding: 0 20px;
}

.portal-card {
  background: #fff;
  border: 1px solid $portal-border;
  border-radius: $portal-radius;
  box-shadow: $portal-shadow;
  transition: box-shadow 0.2s ease, transform 0.2s ease;
}

.portal-link {
  color: $portal-primary;
  cursor: pointer;
  text-decoration: none;
  &:hover { color: $portal-primary-light; }
}

.portal-section-title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 20px;
  font-weight: 700;
  color: $portal-ink;
  margin: 28px 0 16px;
  &::before {
    content: '';
    width: 4px;
    height: 20px;
    border-radius: 2px;
    background: linear-gradient(180deg, $portal-primary-light, $portal-primary);
  }
}

.portal-layout {
  .el-button--primary {
    background: linear-gradient(135deg, $portal-primary, $portal-primary-light);
    border-color: $portal-primary;
    &:hover, &:focus {
      background: linear-gradient(135deg, ${C.primaryHover}, $portal-primary);
      border-color: ${C.primaryHover};
    }
  }
}
`)

require('./apply-portal-theme-home.js')

patchTemplate('layout-portal/components/PortalHeader.vue',
  '<span class="logo-icon">\u5377</span>',
  '<span class="logo-icon"><i class="el-icon-notebook-2" /></span>')

patchStyle('layout-portal/components/PortalHeader.vue', `
$primary: ${C.primary};
$primary-light: ${C.primaryLight};

.portal-topbar {
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(8px);
  border-bottom: 1px solid ${C.tintBorder};
  padding: 16px 0 12px;
}

.topbar-inner {
  display: flex;
  align-items: flex-start;
  gap: 24px;
}

.portal-logo {
  display: flex;
  align-items: center;
  gap: 10px;
  text-decoration: none;
  color: #1e293b;
  font-weight: 700;
  font-size: 20px;
  white-space: nowrap;
}

.logo-icon {
  width: 42px;
  height: 42px;
  border-radius: 12px;
  background: linear-gradient(135deg, $primary, $primary-light);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  box-shadow: 0 4px 12px ${C.shadowMd};
}

.topbar-search { flex: 1; min-width: 0; }

.hot-words { margin-top: 8px; font-size: 12px; color: #94a3b8; }
.hot-word {
  margin-right: 14px;
  color: $primary;
  cursor: pointer;
  &:hover { color: $primary-light; text-decoration: underline; }
}

.topbar-right {
  display: flex;
  align-items: center;
  gap: 8px;
  white-space: nowrap;
  padding-top: 4px;
}

.user-name { color: #64748b; font-size: 13px; margin-right: 4px; }
`)

patchStyle('layout-portal/components/PortalNav.vue', `
$primary: ${C.primary};
$primary-light: ${C.primaryLight};

.portal-nav {
  background: #fff;
  border-bottom: 1px solid ${C.tintBorder};
  box-shadow: 0 2px 8px ${C.shadow};
}

.nav-inner {
  display: flex;
  align-items: center;
  min-height: 52px;
  gap: 8px;
  padding: 8px 0;
}

.subject-dropdown { position: relative; flex-shrink: 0; }

.subject-btn {
  height: 40px;
  min-width: 120px;
  padding: 0 16px;
  border: none;
  border-radius: 10px;
  background: linear-gradient(135deg, $primary, $primary-light);
  color: #fff;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  box-shadow: 0 2px 8px ${C.shadowMd};
}

.subject-panel {
  position: absolute;
  top: calc(100% + 6px);
  left: 0;
  z-index: 2000;
  width: 480px;
  padding: 14px 16px 16px;
  border-radius: 12px;
  box-shadow: 0 12px 40px ${C.shadowMd};
}

.stage-tabs {
  display: flex;
  gap: 6px;
  margin-bottom: 12px;
  padding-bottom: 10px;
  border-bottom: 1px solid #f1f5f9;
}

.stage-tab {
  padding: 5px 14px;
  font-size: 13px;
  color: #64748b;
  cursor: pointer;
  border-radius: 20px;
  &.active { color: ${C.primaryText}; background: ${C.tintActive}; font-weight: 600; }
}

.subject-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 8px;
}

.subject-item {
  padding: 8px;
  font-size: 13px;
  text-align: center;
  border-radius: 8px;
  cursor: pointer;
  color: #334155;
  &:hover { background: ${C.tintHover}; color: $primary; }
  &.active { background: $primary; color: #fff; }
}

.nav-links {
  display: flex;
  align-items: center;
  flex: 1;
  gap: 4px;
  overflow-x: auto;
  padding-left: 8px;
}

.nav-link {
  padding: 8px 16px;
  color: #64748b;
  font-size: 14px;
  text-decoration: none;
  white-space: nowrap;
  border-radius: 20px;
  transition: all 0.15s;
  &:hover { color: $primary; background: ${C.tintHover}; }
  &.active {
    color: #fff;
    font-weight: 600;
    background: linear-gradient(135deg, $primary, $primary-light);
    box-shadow: 0 2px 8px ${C.shadowMd};
  }
}
`)

patchStyle('layout-portal/index.vue', `
.portal-layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: ${C.pageBg};
}

.portal-main { flex: 1; }

.portal-footer {
  background: #fff;
  border-top: 1px solid ${C.tintBorder};
  padding: 20px 0;
  font-size: 13px;
  color: #94a3b8;
  text-align: center;
}

.footer-admin-link {
  margin-left: 16px;
  color: ${C.primary};
  text-decoration: none;
  &:hover { color: ${C.primaryLight}; }
}
`)

patchStyle('layout-portal/components/PortalFloatingBar.vue', `
.portal-float-bar {
  position: fixed;
  right: 20px;
  bottom: 80px;
  top: auto;
  transform: none;
  z-index: 1900;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.float-item {
  width: 56px;
  height: 56px;
  padding: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  font-size: 10px;
  color: #64748b;
  cursor: pointer;
  background: #fff;
  border-radius: 16px;
  border: 1px solid #e2e8f0;
  box-shadow: 0 4px 16px ${C.shadow};
  i { font-size: 22px; display: block; margin-bottom: 2px; }
  &:hover { color: ${C.primary}; border-color: ${C.tintActive}; transform: translateY(-2px); }
}

.float-item.basket {
  color: ${C.accentAmber};
  background: linear-gradient(135deg, #fff8ed, #fff);
  i { color: ${C.accentAmber}; }
}
`)

patchStyle('views/portal/Login.vue', `
$primary: ${C.primary};
$primary-light: ${C.primaryLight};

.portal-login-page {
  display: flex;
  min-height: 100vh;
  background: ${C.pageBg};
}

.login-left {
  flex: 1;
  background: linear-gradient(145deg, ${C.loginFrom} 0%, ${C.loginMid} 50%, ${C.loginTo} 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48px;
  color: ${C.loginText};
  position: relative;
  overflow: hidden;
  &::before {
    content: '';
    position: absolute;
    width: 400px;
    height: 400px;
    border-radius: 50%;
    background: radial-gradient(circle, rgba(110, 189, 212, 0.3), transparent 70%);
    top: -100px;
    right: -100px;
  }
}

.brand-block { max-width: 400px; position: relative; z-index: 1; }

.brand-logo {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  text-decoration: none;
  color: ${C.loginText};
  margin-bottom: 32px;
}

.logo-icon {
  width: 48px;
  height: 48px;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.15);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
}

.logo-text { font-size: 24px; font-weight: 700; }
.brand-block h1 { margin: 0 0 12px; font-size: 26px; font-weight: 600; }
.brand-desc { margin: 0 0 28px; font-size: 15px; opacity: 0.85; }

.brand-features {
  list-style: none;
  padding: 0;
  margin: 0;
  li {
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 8px 0;
    font-size: 14px;
    opacity: 0.9;
    i { font-size: 16px; color: #b8e4f0; }
  }
}

.login-right {
  width: 460px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 32px;
  background: #fff;
}

.login-card { width: 100%; max-width: 360px; }
.card-title { margin: 0 0 6px; font-size: 24px; font-weight: 700; color: #1e293b; }
.card-subtitle { margin: 0 0 28px; font-size: 13px; color: #94a3b8; }

.login-form .el-input ::v-deep input { height: 42px; border-radius: 10px; }
.code-row { display: flex; gap: 10px; .el-input { flex: 1; } }
.code-img { height: 42px; border-radius: 10px; cursor: pointer; border: 1px solid #e2e8f0; }
.form-options { margin-bottom: 20px; }

.login-btn {
  width: 100%;
  height: 44px;
  font-size: 15px;
  border-radius: 10px;
  background: linear-gradient(135deg, $primary, $primary-light);
  border-color: $primary;
  &:hover, &:focus { background: linear-gradient(135deg, ${C.primaryHover}, $primary); border-color: ${C.primaryHover}; }
}

.login-links {
  display: flex;
  justify-content: space-between;
  margin-top: 24px;
  font-size: 13px;
  a { color: $primary; text-decoration: none; &:hover { color: $primary-light; } }
  .admin-link { color: #94a3b8; }
}

@media (max-width: 900px) {
  .portal-login-page { flex-direction: column; }
  .login-left { padding: 32px 24px; }
  .login-right { width: 100%; padding: 24px; }
}
`)

patchStyle('views/portal/Paper.vue', `
$primary: ${C.primary};
$primary-light: ${C.primaryLight};

.paper-wrap { padding: 24px 16px 40px; }

.paper-head {
  padding: 36px 32px;
  text-align: center;
  margin-bottom: 16px;
  h2 { margin: 0 0 8px; color: #1e293b; font-size: 24px; font-weight: 700; }
  p { color: #64748b; margin: 0 0 20px; }
}

.paper-actions { display: flex; justify-content: center; gap: 12px; }

.paper-tip {
  margin-top: 16px;
  font-size: 14px;
  color: #64748b;
  b { color: $primary; font-size: 20px; font-weight: 700; }
}

.paper-steps { padding: 8px 32px 24px; }

.step-item {
  display: flex;
  gap: 16px;
  padding: 18px 0;
  border-bottom: 1px solid #f1f5f9;
  &:last-child { border-bottom: none; }
}

.step-num {
  width: 40px;
  height: 40px;
  border-radius: 12px;
  background: linear-gradient(135deg, ${C.tintBorder}, ${C.tintActive});
  color: ${C.primaryText};
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.step-body {
  h4 { margin: 0 0 4px; font-size: 15px; color: #1e293b; font-weight: 600; }
  p { margin: 0; font-size: 13px; color: #64748b; line-height: 1.5; }
}
`)

patchStyle('views/portal/QuestionDetail.vue', `
$primary: ${C.primary};

.portal-detail { padding: 16px 0 40px; }

.breadcrumb {
  font-size: 13px;
  color: #94a3b8;
  margin-bottom: 14px;
  a { color: $primary; text-decoration: none; font-weight: 500; &:hover { color: ${C.primaryLight}; } }
  .sep { margin: 0 8px; color: #cbd5e1; }
}

.detail-panel { padding: 24px 28px; border-radius: 12px; }

.detail-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: center;
  margin-bottom: 16px;
  font-size: 13px;
  color: #64748b;
}

.detail-stem { margin-bottom: 16px; line-height: 1.7; }

.detail-options {
  list-style: none;
  padding: 0;
  margin: 0 0 20px;
  li {
    display: flex;
    gap: 8px;
    padding: 8px 12px;
    font-size: 14px;
    border-radius: 8px;
    margin-bottom: 4px;
    &:hover { background: ${C.tintHover}; }
  }
  .opt-label { font-weight: 600; color: $primary; min-width: 24px; }
}

.detail-actions {
  display: flex;
  gap: 12px;
  padding-top: 16px;
  border-top: 1px solid #f1f5f9;
}
`)

console.log('portal theme complete')
