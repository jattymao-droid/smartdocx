/**
 * MathLive locale + categorized formula templates for question bank editors.
 */
let configured = false

/** Formula quick-insert groups: math / physics / chemistry. */
export const FORMULA_TEMPLATE_GROUPS = [
  {
    key: 'math',
    label: '数学',
    templates: [
      { key: 'frac', label: '分数', latex: '\\frac{#?}{#?}', preview: '\\frac{a}{b}' },
      { key: 'sqrt', label: '根号', latex: '\\sqrt{#?}', preview: '\\sqrt{x}' },
      { key: 'nthroot', label: 'n次根', latex: '\\sqrt[#?]{#?}', preview: '\\sqrt[n]{x}' },
      { key: 'sup', label: '上标', latex: '^{#?}', preview: 'x^{2}' },
      { key: 'sub', label: '下标', latex: '_{#?}', preview: 'x_{i}' },
      { key: 'abs', label: '绝对值', latex: '\\left|#?\\right|', preview: '\\left|x\\right|' },
      { key: 'paren', label: '括号', latex: '\\left(#?\\right)', preview: '\\left(x\\right)' },
      { key: 'vec', label: '向量', latex: '\\vec{#?}', preview: '\\vec{v}' },
      { key: 'overline', label: '平均', latex: '\\overline{#?}', preview: '\\overline{x}' },
      { key: 'sum', label: '求和', latex: '\\sum_{#?}^{#?}', preview: '\\sum' },
      { key: 'integral', label: '积分', latex: '\\int_{#?}^{#?}', preview: '\\int' },
      { key: 'limit', label: '极限', latex: '\\lim_{#? \\to #?}', preview: '\\lim' },
      { key: 'derivative', label: '导数', latex: '\\dfrac{\\mathrm{d}}{\\mathrm{d}x}', preview: '\\frac{dy}{dx}' },
      { key: 'log', label: '对数', latex: '\\log_{#?}(#?)', preview: '\\log' },
      { key: 'angle', label: '角度', latex: '\\angle #?', preview: '\\angle' },
      { key: 'degree', label: '度', latex: '^{\\circ}', preview: '90^{\\circ}' },
      { key: 'neq', label: '≠', latex: '\\neq', preview: '\\neq' },
      { key: 'ge', label: '≥', latex: '\\ge', preview: '\\ge' },
      { key: 'le', label: '≤', latex: '\\le', preview: '\\le' },
      { key: 'infty', label: '∞', latex: '\\infty', preview: '\\infty' },
      { key: 'matrix', label: '矩阵', latex: '\\begin{pmatrix}#?&#?\\\\#?&#?\\end{pmatrix}', preview: '\\begin{pmatrix}a&b\\\\c&d\\end{pmatrix}' },
      { key: 'cases', label: '分段', latex: '\\begin{cases}#?\\\\#?\\end{cases}', preview: '\\begin{cases}x\\\\y\\end{cases}' },
      { key: 'binom', label: '组合', latex: '\\binom{#?}{#?}', preview: '\\binom{n}{k}' },
      { key: 'parallel', label: '平行', latex: '\\parallel', preview: '\\parallel' },
      { key: 'perp', label: '垂直', latex: '\\perp', preview: '\\perp' },
      { key: 'triangle', label: '三角形', latex: '\\triangle', preview: '\\triangle' },
      { key: 'therefore', label: '因此', latex: '\\therefore', preview: '\\therefore' },
      { key: 'approx', label: '≈', latex: '\\approx', preview: '\\approx' },
      { key: 'subset', label: '子集', latex: '\\subset', preview: '\\subset' },
      { key: 'in', label: '∈', latex: '\\in', preview: '\\in' },
      { key: 'supset', label: '超集', latex: '\\supset', preview: '\\supset' },
      { key: 'cup', label: '并集', latex: '\\cup', preview: '\\cup' },
      { key: 'cap', label: '交集', latex: '\\cap', preview: '\\cap' },
      { key: 'emptyset', label: '空集', latex: '\\emptyset', preview: '\\emptyset' },
      { key: 'notin', label: '不属于', latex: '\\notin', preview: '\\notin' },
      { key: 'forall', label: '任意', latex: '\\forall', preview: '\\forall' },
      { key: 'exists', label: '存在', latex: '\\exists', preview: '\\exists' },
      { key: 'ln', label: '自然对数', latex: '\\ln(#?)', preview: '\\ln x' },
      { key: 'lg', label: '常用对数', latex: '\\lg(#?)', preview: '\\lg x' },
      { key: 'exp-e', label: '指数 e^x', latex: 'e^{#?}', preview: 'e^{x}' },
      { key: 'pm', label: '正负', latex: '\\pm', preview: '\\pm' },
      { key: 'cdot', label: '点乘', latex: '\\cdot', preview: '\\cdot' },
      { key: 'times-m', label: '乘号', latex: '\\times', preview: '\\times' },
      { key: 'div-m', label: '除号', latex: '\\div', preview: '\\div' },
      { key: 'equiv', label: '恒等于', latex: '\\equiv', preview: '\\equiv' },
      { key: 'mod-cong', label: '同余', latex: 'a\\equiv b\\pmod{#?}', preview: 'a\\equiv b' },
      { key: 'prod', label: '连乘', latex: '\\prod_{#?}^{#?}', preview: '\\prod' },
      { key: 'partial', label: '偏导', latex: '\\dfrac{\\partial}{\\partial x}', preview: '\\frac{\\partial}{\\partial x}' },
      { key: 'nabla', label: '梯度', latex: '\\nabla', preview: '\\nabla' },
      { key: 'cdots', label: '省略号', latex: '\\cdots', preview: '\\cdots' },
      { key: 'factorial', label: '阶乘', latex: '{#?}!', preview: 'n!' },
      { key: 'quadratic', label: '求根公式', latex: 'x=\\frac{-b\\pm\\sqrt{b^2-4ac}}{2a}', preview: 'x=\\frac{-b\\pm\\sqrt{b^2-4ac}}{2a}' },
      { key: 'pythag', label: '勾股定理', latex: 'a^2+b^2=c^2', preview: 'a^2+b^2=c^2' },
      { key: 'percent', label: '百分号', latex: '\\%', preview: '\\%' },
      { key: 'overrightarrow', label: '向量箭头', latex: '\\overrightarrow{#?}', preview: '\\overrightarrow{AB}' },
      { key: 'hat-u', label: '单位向量', latex: '\\hat{#?}', preview: '\\hat{i}' },
      { key: 'min', label: '最小值', latex: '\\min', preview: '\\min' },
      { key: 'max', label: '最大值', latex: '\\max', preview: '\\max' },
      { key: 'gcd', label: '最大公因数', latex: '\\gcd', preview: '\\gcd' },
      { key: 'lcm', label: '最小公倍数', latex: '\\mathrm{lcm}', preview: '\\mathrm{lcm}' },
    ]
  },
  {
    key: 'trig',
    label: '三角函数',
    templates: [
      { key: 'sin', label: '正弦', latex: '\\sin(#?)', preview: '\\sin\\theta' },
      { key: 'cos', label: '余弦', latex: '\\cos(#?)', preview: '\\cos\\theta' },
      { key: 'tan', label: '正切', latex: '\\tan(#?)', preview: '\\tan\\theta' },
      { key: 'cot', label: '余切', latex: '\\cot(#?)', preview: '\\cot\\theta' },
      { key: 'sec', label: '正剩', latex: '\\sec(#?)', preview: '\\sec\\theta' },
      { key: 'csc', label: '余剩', latex: '\\csc(#?)', preview: '\\csc\\theta' },
      { key: 'asin', label: '反正弦', latex: '\\arcsin(#?)', preview: '\\arcsin x' },
      { key: 'acos', label: '反余弦', latex: '\\arccos(#?)', preview: '\\arccos x' },
      { key: 'atan', label: '反正切', latex: '\\arctan(#?)', preview: '\\arctan x' },
      { key: 'sin2', label: 'sin²', latex: '\\sin^2\\theta', preview: '\\sin^2\\theta' },
      { key: 'cos2', label: 'cos²', latex: '\\cos^2\\theta', preview: '\\cos^2\\theta' },
      { key: 'sin-cos', label: '恒等式', latex: '\\sin^2\\theta+\\cos^2\\theta=1', preview: '\\sin^2\\theta+\\cos^2\\theta=1' },
      { key: 'double-angle', label: '二倍角', latex: '\\sin 2\\theta=2\\sin\\theta\\cos\\theta', preview: '\\sin 2\\theta' },
      { key: 'pi-radian', label: '弧度', latex: '\\pi', preview: '\\pi' },
      { key: 'degree-trig', label: '角度制', latex: '^{\\circ}', preview: '30^{\\circ}' },
    ]
  },
  {
    key: 'greek',
    label: '希腊字母',
    templates: [
      { key: 'alpha', label: 'α 阿尔法', latex: '\\alpha', preview: '\\alpha' },
      { key: 'beta', label: 'β 贝塔', latex: '\\beta', preview: '\\beta' },
      { key: 'gamma', label: 'γ 伽马', latex: '\\gamma', preview: '\\gamma' },
      { key: 'delta', label: 'δ 德尔塔', latex: '\\delta', preview: '\\delta' },
      { key: 'epsilon', label: 'ε 艾普西隆', latex: '\\epsilon', preview: '\\epsilon' },
      { key: 'varepsilon', label: 'ε 变体', latex: '\\varepsilon', preview: '\\varepsilon' },
      { key: 'zeta', label: 'ζ 扎塔', latex: '\\zeta', preview: '\\zeta' },
      { key: 'eta', label: 'η 埃塔', latex: '\\eta', preview: '\\eta' },
      { key: 'theta', label: 'θ 西塔', latex: '\\theta', preview: '\\theta' },
      { key: 'lambda', label: 'λ 兰姆达', latex: '\\lambda', preview: '\\lambda' },
      { key: 'mu', label: 'μ 缪', latex: '\\mu', preview: '\\mu' },
      { key: 'nu', label: 'ν 润', latex: '\\nu', preview: '\\nu' },
      { key: 'pi', label: 'π 派', latex: '\\pi', preview: '\\pi' },
      { key: 'rho', label: 'ρ 肉', latex: '\\rho', preview: '\\rho' },
      { key: 'sigma', label: 'σ 西格马', latex: '\\sigma', preview: '\\sigma' },
      { key: 'tau', label: 'τ 陶', latex: '\\tau', preview: '\\tau' },
      { key: 'phi', label: 'φ 斐', latex: '\\phi', preview: '\\phi' },
      { key: 'varphi', label: 'φ 变体', latex: '\\varphi', preview: '\\varphi' },
      { key: 'omega', label: 'ω 欧米伽', latex: '\\omega', preview: '\\omega' },
      { key: 'Delta', label: 'Δ 大Δ', latex: '\\Delta', preview: '\\Delta' },
      { key: 'Sigma', label: 'Σ 大Σ', latex: '\\Sigma', preview: '\\Sigma' },
      { key: 'Omega', label: 'Ω 大Ω', latex: '\\Omega', preview: '\\Omega' },
      { key: 'Pi', label: 'Π 大Π', latex: '\\Pi', preview: '\\Pi' },
      { key: 'Phi', label: 'Φ 大Φ', latex: '\\Phi', preview: '\\Phi' },
      { key: 'Theta', label: 'Θ 大Θ', latex: '\\Theta', preview: '\\Theta' },
      { key: 'Lambda', label: 'Λ 大Λ', latex: '\\Lambda', preview: '\\Lambda' },
      { key: 'Gamma', label: 'Γ 大Γ', latex: '\\Gamma', preview: '\\Gamma' },
      { key: 'kappa', label: 'κ 卡帕', latex: '\\kappa', preview: '\\kappa' },
      { key: 'xi', label: 'ξ 克西', latex: '\\xi', preview: '\\xi' },
      { key: 'psi', label: 'ψ 普西', latex: '\\psi', preview: '\\psi' },
      { key: 'chi', label: 'χ 卡', latex: '\\chi', preview: '\\chi' },
    ]
  },
  {
    key: 'geometry',
    label: '几何',
    templates: [
      { key: 'cong', label: '全等', latex: '\\cong', preview: '\\cong' },
      { key: 'sim-geom', label: '相似', latex: '\\backsim', preview: '\\backsim' },
      { key: 'measuredangle', label: '角', latex: '\\measuredangle', preview: '\\measuredangle' },
      { key: 'triangle-abc', label: '三角形', latex: '\\triangle ABC', preview: '\\triangle ABC' },
      { key: 'square', label: '正方形', latex: '\\square', preview: '\\square' },
      { key: 'odot', label: '圆', latex: '\\odot', preview: '\\odot' },
      { key: 'overparen', label: '弧', latex: '\\overparen{#?}', preview: '\\overparen{AB}' },
      { key: 'overline-seg', label: '线段', latex: '\\overline{#?}', preview: '\\overline{AB}' },
      { key: 'ratio', label: '比', latex: 'a:b', preview: 'a:b' },
      { key: 'perp-bisector', label: '垂直平分', latex: '\\perp', preview: '\\perp' },
      { key: 'circ-area', label: '圆面积', latex: 'S=\\pi r^2', preview: 'S=\\pi r^2' },
      { key: 'circ-circ', label: '圆周长', latex: 'C=2\\pi r', preview: 'C=2\\pi r' },
      { key: 'tri-area', label: '三角面积', latex: 'S=\\frac{1}{2}ah', preview: 'S=\\frac{1}{2}ah' },
      { key: 'pyth-geom', label: '勾股', latex: 'a^2+b^2=c^2', preview: 'a^2+b^2=c^2' },
      { key: 'similar-ratio', label: '相似比', latex: '\\frac{a}{b}=\\frac{c}{d}', preview: '\\frac{a}{b}=\\frac{c}{d}' },
    ]
  },
  {
    key: 'statistics',
    label: '统计概率',
    templates: [
      { key: 'prob', label: '概率', latex: 'P(#?)', preview: 'P(A)' },
      { key: 'prob-cond', label: '条件概率', latex: 'P(A|B)', preview: 'P(A|B)' },
      { key: 'mean', label: '平均数', latex: '\\bar{x}', preview: '\\bar{x}' },
      { key: 'mean-y', label: '均值', latex: '\\bar{y}', preview: '\\bar{y}' },
      { key: 'variance', label: '方差', latex: 's^2', preview: 's^2' },
      { key: 'std-dev', label: '标准差', latex: '\\sigma', preview: '\\sigma' },
      { key: 'sum-xi', label: '求和', latex: '\\sum_{i=1}^{n}x_i', preview: '\\sum x_i' },
      { key: 'binom-coef', label: '组合数', latex: '\\binom{n}{k}', preview: '\\binom{n}{k}' },
      { key: 'permute', label: '排列', latex: 'A_n^m', preview: 'A_n^m' },
      { key: 'comb', label: '组合', latex: 'C_n^k', preview: 'C_n^k' },
      { key: 'mu-pop', label: '总体均值', latex: '\\mu', preview: '\\mu' },
      { key: 'expect', label: '期望', latex: 'E(X)', preview: 'E(X)' },
      { key: 'percentile', label: '百分位', latex: 'P_{#?}', preview: 'P_{50}' },
      { key: 'freq', label: '频率', latex: 'f', preview: 'f' },
    ]
  },
  {
    key: 'physics',
    label: '物理',
    templates: [
      { key: 'vel', label: '速度', latex: '\\vec{v}', preview: '\\vec{v}' },
      { key: 'acc', label: '加速度', latex: '\\vec{a}', preview: '\\vec{a}' },
      { key: 'force', label: '力', latex: '\\vec{F}', preview: '\\vec{F}' },
      { key: 'f-ma', label: 'F=ma', latex: 'F=ma', preview: 'F=ma' },
      { key: 'momentum', label: '动量', latex: '\\vec{p}=m\\vec{v}', preview: '\\vec{p}' },
      { key: 'energy-k', label: '动能', latex: 'E_k=\\frac{1}{2}mv^2', preview: 'E_k' },
      { key: 'energy-p', label: '势能', latex: 'E_p=#?', preview: 'E_p' },
      { key: 'work', label: '功', latex: 'W=#?', preview: 'W' },
      { key: 'power', label: '功率', latex: 'P=\\frac{W}{t}', preview: 'P' },
      { key: 'pressure', label: '压强', latex: 'p=\\frac{F}{S}', preview: 'p' },
      { key: 'density', label: '密度', latex: '\\rho=\\frac{m}{V}', preview: '\\rho' },
      { key: 'wavelength', label: '波长', latex: '\\lambda', preview: '\\lambda' },
      { key: 'frequency', label: '频率', latex: 'f', preview: 'f' },
      { key: 'omega', label: '角速度', latex: '\\omega', preview: '\\omega' },
      { key: 'period', label: '周期', latex: 'T', preview: 'T' },
      { key: 'e-field', label: '电场', latex: '\\vec{E}', preview: '\\vec{E}' },
      { key: 'b-field', label: '磁场', latex: '\\vec{B}', preview: '\\vec{B}' },
      { key: 'current', label: '电流', latex: 'I', preview: 'I' },
      { key: 'voltage', label: '电压', latex: 'U', preview: 'U' },
      { key: 'resistance', label: '电阻', latex: 'R', preview: 'R' },
      { key: 'ohm', label: '欧姆', latex: '\\Omega', preview: '\\Omega' },
      { key: 'coulomb', label: '库伦', latex: 'q', preview: 'q' },
      { key: 'capacitance', label: '电容', latex: 'C', preview: 'C' },
      { key: 'gravity', label: '万有吸引', latex: 'G', preview: 'G' },
      { key: 'g', label: '重力加速度', latex: 'g', preview: 'g' },
      { key: 'planck', label: '普朗克常数', latex: 'h', preview: 'h' },
      { key: 'light-speed', label: '光速', latex: 'c', preview: 'c' },
      { key: 'unit-ms', label: 'm/s', latex: '\\mathrm{m/s}', preview: '\\mathrm{m/s}' },
      { key: 'unit-kg', label: 'kg', latex: '\\mathrm{kg}', preview: '\\mathrm{kg}' },
      { key: 'unit-n', label: 'N', latex: '\\mathrm{N}', preview: '\\mathrm{N}' },
      { key: 's-dis', label: '位移', latex: 's=#?', preview: 's' },
      { key: 'v-at', label: '加速速度', latex: 'v=v_0+at', preview: 'v=v_0+at' },
      { key: 's-at2', label: '位移公式', latex: 's=v_0 t+\\frac{1}{2}at^2', preview: 's=vt' },
      { key: 'hookes', label: '胡克定律', latex: 'F=kx', preview: 'F=kx' },
      { key: 'v-flambda', label: '波速', latex: 'v=f\\lambda', preview: 'v=f\\lambda' },
      { key: 'p-ui', label: '电功率', latex: 'P=UI', preview: 'P=UI' },
      { key: 'e-mc2', label: '质能方程', latex: 'E=mc^2', preview: 'E=mc^2' },
      { key: 'w-fs', label: '功的公式', latex: 'W=Fs', preview: 'W=Fs' },
      { key: 'centripetal', label: '向心力', latex: 'F=\\frac{mv^2}{r}', preview: 'F=\\frac{mv^2}{r}' },
      { key: 'q-mcdt', label: '热量', latex: 'Q=cm\\Delta t', preview: 'Q=cm\\Delta t' },
      { key: 'n-refract', label: '折射率', latex: 'n=\\frac{c}{v}', preview: 'n=\\frac{c}{v}' },
      { key: 'v-wr', label: '线速度', latex: 'v=\\omega r', preview: 'v=\\omega r' },
      { key: 'qvB', label: '洛伦兹力', latex: 'F=qvB', preview: 'F=qvB' },
      { key: 'snell', label: '折射定律', latex: 'n_1\\sin\\theta_1=n_2\\sin\\theta_2', preview: 'n\\sin\\theta' },
      { key: 'ideal-gas', label: '理想气体', latex: 'pV=nRT', preview: 'pV=nRT' },
      { key: 'buoyancy', label: '浮力', latex: 'F=\\rho g V', preview: 'F=\\rho g V' },
      { key: 'unit-j', label: 'J', latex: '\\mathrm{J}', preview: '\\mathrm{J}' },
      { key: 'unit-w', label: 'W', latex: '\\mathrm{W}', preview: '\\mathrm{W}' },
      { key: 'unit-v', label: 'V', latex: '\\mathrm{V}', preview: '\\mathrm{V}' },
      { key: 'unit-a', label: 'A', latex: '\\mathrm{A}', preview: '\\mathrm{A}' },
      { key: 'unit-pa', label: 'Pa', latex: '\\mathrm{Pa}', preview: '\\mathrm{Pa}' },
      { key: 'unit-hz', label: 'Hz', latex: '\\mathrm{Hz}', preview: '\\mathrm{Hz}' },
      { key: 'unit-mol', label: 'mol', latex: '\\mathrm{mol}', preview: '\\mathrm{mol}' },
    ]
  },
  {
    key: 'chemistry',
    label: '化学',
    templates: [
      { key: 'mol-formula', label: '分子式', latex: '\\mathrm{#?}_{#?}', preview: '\\mathrm{H}_{2}\\mathrm{O}' },
      { key: 'ion', label: '离子', latex: '\\mathrm{#?}^{#?}', preview: '\\mathrm{Na}^{+}' },
      { key: 'ion-sub', label: '离子(下标)', latex: '\\mathrm{#?}_{#?}^{#?}', preview: '\\mathrm{SO}_{4}^{2-}' },
      { key: 'arrow-r', label: '反应→', latex: '\\rightarrow', preview: '\\rightarrow' },
      { key: 'arrow-eq', label: '可逆⇌', latex: '\\rightleftharpoons', preview: '\\rightleftharpoons' },
      { key: 'gas', label: '气体↑', latex: '\\uparrow', preview: '\\uparrow' },
      { key: 'precip', label: '沉淀↓', latex: '\\downarrow', preview: '\\downarrow' },
      { key: 'heat', label: '加热△', latex: '\\triangle', preview: '\\triangle' },
      { key: 'catalyst', label: '催化剂', latex: '\\xrightarrow{\\mathrm{#?}}', preview: '\\xrightarrow{}' },
      { key: 'conc', label: '浓度[]', latex: '[\\mathrm{#?}]', preview: '[\\mathrm{H}^{+}]' },
      { key: 'ph', label: 'pH', latex: '\\mathrm{pH}', preview: '\\mathrm{pH}' },
      { key: 'mol', label: '摩尔', latex: '\\mathrm{mol}', preview: '\\mathrm{mol}' },
      { key: 'h2o', label: '水', latex: '\\mathrm{H}_2\\mathrm{O}', preview: '\\mathrm{H}_{2}\\mathrm{O}' },
      { key: 'co2', label: 'CO₂', latex: '\\mathrm{CO}_2', preview: '\\mathrm{CO}_{2}' },
      { key: 'h2so4', label: '硫酸', latex: '\\mathrm{H}_2\\mathrm{SO}_4', preview: '\\mathrm{H}_{2}\\mathrm{SO}_{4}' },
      { key: 'naoh', label: 'NaOH', latex: '\\mathrm{NaOH}', preview: '\\mathrm{NaOH}' },
      { key: 'hcl', label: '盐酸', latex: '\\mathrm{HCl}', preview: '\\mathrm{HCl}' },
      { key: 'nacl', label: '食盐', latex: '\\mathrm{NaCl}', preview: '\\mathrm{NaCl}' },
      { key: 'yield', label: '产率η', latex: '\\eta', preview: '\\eta' },
      { key: 'delta-h', label: '焓变', latex: '\\Delta H', preview: '\\Delta H' },
      { key: 'equilibrium', label: '平衡常数', latex: 'K', preview: 'K' },
      { key: 'electron', label: '电子', latex: 'e^-', preview: 'e^{-}' },
      { key: 'oxidation', label: '氧化', latex: '\\mathrm{O}', preview: '\\mathrm{O}' },
      { key: 'reduction', label: '还原', latex: '\\mathrm{H}', preview: '\\mathrm{H}' },
      { key: 'ksp', label: '溶度积', latex: 'K_{sp}', preview: 'K_{sp}' },
      { key: 'kw', label: '水的离子积', latex: 'K_w', preview: 'K_w' },
      { key: 'ka', label: '电离常数', latex: 'K_a', preview: 'K_a' },
      { key: 'kb', label: '碱常数', latex: 'K_b', preview: 'K_b' },
      { key: 'delta-g', label: '吉布斯能', latex: '\\Delta G', preview: '\\Delta G' },
      { key: 'delta-s', label: '熵变', latex: '\\Delta S', preview: '\\Delta S' },
      { key: 'o2', label: '氧气', latex: '\\mathrm{O}_2', preview: '\\mathrm{O}_{2}' },
      { key: 'h2', label: '氢气', latex: '\\mathrm{H}_2', preview: '\\mathrm{H}_{2}' },
      { key: 'n2', label: '氮气', latex: '\\mathrm{N}_2', preview: '\\mathrm{N}_{2}' },
      { key: 'nh3', label: '氨气', latex: '\\mathrm{NH}_3', preview: '\\mathrm{NH}_{3}' },
      { key: 'caco3', label: '碳酸钙', latex: '\\mathrm{CaCO}_3', preview: '\\mathrm{CaCO}_{3}' },
      { key: 'fe2', label: '亚铁离子', latex: '\\mathrm{Fe}^{2+}', preview: '\\mathrm{Fe}^{2+}' },
      { key: 'fe3', label: '铁离子', latex: '\\mathrm{Fe}^{3+}', preview: '\\mathrm{Fe}^{3+}' },
      { key: 'cu2', label: '铜离子', latex: '\\mathrm{Cu}^{2+}', preview: '\\mathrm{Cu}^{2+}' },
      { key: 'ag-plus', label: '银离子', latex: '\\mathrm{Ag}^{+}', preview: '\\mathrm{Ag}^{+}' },
      { key: 'molar-m', label: '摩尔质量', latex: 'M', preview: 'M' },
      { key: 'n-m-M', label: '物质的量', latex: 'n=\\frac{m}{M}', preview: 'n=\\frac{m}{M}' },
      { key: 'c-n-V', label: '物质的量浓度', latex: 'c=\\frac{n}{V}', preview: 'c=\\frac{n}{V}' },
      { key: 'charge-plus', label: '正电荷', latex: '^{+}', preview: '^{+}' },
      { key: 'charge-minus', label: '负电荷', latex: '^{-}', preview: '^{-}' },
      { key: 'oxidation-num', label: '氧化数', latex: '\\overset{#?}{#?}', preview: '\\overset{+2}{Fe}' },
      { key: 'ce-template', label: 'mhchem', latex: '\\ce{#?}', preview: '\\ce{H2O}' },
    ]
  },
  {
    key: 'organic',
    label: '有机化学',
    templates: [
      { key: 'org-alkyl', label: '烃基 R', latex: '\\mathrm{R}', preview: '\\mathrm{R}' },
      { key: 'org-phenyl', label: '苯基', latex: '\\mathrm{C_6H_5-}', preview: '\\mathrm{C_6H_5\\text{-}}' },
      { key: 'org-aryl', label: '芳基 Ar', latex: '\\mathrm{Ar-}', preview: '\\mathrm{Ar\\text{-}}' },
      { key: 'org-methyl', label: '甲基', latex: '\\mathrm{-CH_3}', preview: '\\mathrm{-CH_3}' },
      { key: 'org-ethyl', label: '乙基', latex: '\\mathrm{-C_2H_5}', preview: '\\mathrm{-C_2H_5}' },
      { key: 'org-methane', label: '甲烷', latex: '\\mathrm{CH_4}', preview: '\\ce{CH4}' },
      { key: 'org-ethene', label: '乙烯', latex: '\\mathrm{CH_2=CH_2}', preview: '\\ce{H2C=CH2}' },
      { key: 'org-ethyne', label: '乙炔', latex: '\\mathrm{CH\\equiv CH}', preview: '\\ce{HC#CH}' },
      { key: 'org-benzene', label: '苯', latex: '\\mathrm{C_6H_6}', preview: '\\ce{C6H6}' },
      { key: 'org-single-bond', label: '单键', latex: '\\mathrm{C-C}', preview: '\\mathrm{C\\text{-}C}' },
      { key: 'org-double-bond', label: '双键', latex: '\\mathrm{C=C}', preview: '\\mathrm{C=C}' },
      { key: 'org-triple-bond', label: '三键', latex: '\\mathrm{C\\equiv C}', preview: '\\mathrm{C\\equiv C}' },
      { key: 'org-carbonyl', label: '羰基', latex: '\\mathrm{C=O}', preview: '\\mathrm{C=O}' },
      { key: 'org-hydroxyl', label: '羟基', latex: '\\mathrm{R-OH}', preview: '\\mathrm{R\\text{-}OH}' },
      { key: 'org-carboxyl', label: '羧基', latex: '\\mathrm{R-COOH}', preview: '\\mathrm{R\\text{-}COOH}' },
      { key: 'org-aldehyde', label: '醛基', latex: '\\mathrm{R-CHO}', preview: '\\mathrm{R\\text{-}CHO}' },
      { key: 'org-ketone', label: '酮基', latex: '\\mathrm{R-CO-R^{\\prime}}', preview: '\\mathrm{RCOR^{\\prime}}' },
      { key: 'org-amino', label: '氨基', latex: '\\mathrm{R-NH_2}', preview: '\\mathrm{R\\text{-}NH_2}' },
      { key: 'org-amide', label: '酰胺', latex: '\\mathrm{R-CONH_2}', preview: '\\mathrm{R\\text{-}CONH_2}' },
      { key: 'org-ester', label: '酯基', latex: '\\mathrm{R-COO-R^{\\prime}}', preview: '\\mathrm{RCOOR^{\\prime}}' },
      { key: 'org-ether', label: '醚键', latex: '\\mathrm{R-O-R^{\\prime}}', preview: '\\mathrm{ROR^{\\prime}}' },
      { key: 'org-halo', label: '卤代', latex: '\\mathrm{R-X}', preview: '\\mathrm{R\\text{-}X}' },
      { key: 'org-nitro', label: '硝基', latex: '\\mathrm{R-NO_2}', preview: '\\mathrm{R\\text{-}NO_2}' },
      { key: 'org-nitrile', label: '腈基', latex: '\\mathrm{R-CN}', preview: '\\mathrm{R\\text{-}CN}' },
      { key: 'org-thiol', label: '巯基', latex: '\\mathrm{R-SH}', preview: '\\mathrm{R\\text{-}SH}' },
      { key: 'org-sulfonic', label: '磺酸基', latex: '\\mathrm{R-SO_3H}', preview: '\\mathrm{R\\text{-}SO_3H}' },
      { key: 'org-ethanol', label: '乙醇', latex: '\\mathrm{C_2H_5OH}', preview: '\\ce{C2H5OH}' },
      { key: 'org-acetic', label: '乙酸', latex: '\\mathrm{CH_3COOH}', preview: '\\ce{CH3COOH}' },
      { key: 'org-formaldehyde', label: '甲醛', latex: '\\mathrm{HCHO}', preview: '\\ce{HCHO}' },
      { key: 'org-acetone', label: '丙酮', latex: '\\mathrm{CH_3COCH_3}', preview: '\\ce{CH3COCH3}' },
      { key: 'org-benzene-ol', label: '苯酚', latex: '\\mathrm{C_6H_5OH}', preview: '\\ce{C6H5OH}' },
      { key: 'org-glucose', label: '葡萄糖', latex: '\\mathrm{C_6H_{12}O_6}', preview: '\\ce{C6H12O6}' },
      { key: 'org-urea', label: '尿素', latex: '\\mathrm{CO(NH_2)_2}', preview: '\\ce{CO(NH2)2}' },
      { key: 'org-starch', label: '淀粉', latex: '(\\mathrm{C_6H_{10}O_5})_n', preview: '\\ce{(C6H10O5)_n}' },
      { key: 'org-esterification', label: '酯化反应', latex: '\\mathrm{R-COOH+R^{\\prime}OH\\rightleftharpoons R-COOR^{\\prime}+H_2O}', preview: '\\ce{RCOOH + ROH <=> RCOOR}' },
      { key: 'org-propene', label: '丙烯', latex: '\\mathrm{CH_3CH=CH_2}', preview: '\\ce{CH3CH=CH2}' },
      { key: 'org-propylene', label: '丁烷', latex: '\\mathrm{CH_3CH_2CH_2CH_3}', preview: '\\ce{CH3CH2CH2CH3}' },
      { key: 'org-glycerol', label: '甘油', latex: '\\mathrm{C_3H_5(OH)_3}', preview: '\\ce{C3H5(OH)3}' },
      { key: 'org-amino-acid', label: '氨基酸', latex: '\\mathrm{H_2N-CHR-COOH}', preview: '\\mathrm{H_2N\\text{-}CHR\\text{-}COOH}' },
      { key: 'org-peptide', label: '肽键', latex: '\\mathrm{-CO-NH-}', preview: '\\mathrm{\\text{-}CO\\text{-}NH\\text{-}}' },
      { key: 'org-markovnikov', label: '马氏规则', latex: '\\mathrm{HBr}', preview: '\\mathrm{HBr}' },
      { key: 'org-zn-reduction', label: '还原反应', latex: '\\mathrm{Zn/HCl}', preview: '\\mathrm{Zn/HCl}' },
      { key: 'org-naoh-hydro', label: '碱性水解', latex: '\\mathrm{NaOH/H_2O}', preview: '\\mathrm{NaOH}' },
      { key: 'org-kmno4', label: '高锬酸钾', latex: '\\mathrm{KMnO_4}', preview: '\\mathrm{KMnO_4}' },
      { key: 'org-h2-pd', label: '催化加氢', latex: '\\mathrm{H_2/Pd}', preview: '\\mathrm{H_2/Pd}' },
      { key: 'org-na-ethanol', label: '乙醇钠', latex: '\\mathrm{Na/C_2H_5OH}', preview: '\\mathrm{Na}' },
      { key: 'org-h2so4-conc', label: '浓硫酸', latex: '\\mathrm{H_2SO_4}', preview: '\\mathrm{H_2SO_4}' },
      { key: 'org-h2so4-dil', label: '稀硫酸', latex: '\\mathrm{H_2SO_4}', preview: '\\mathrm{H_2SO_4}' },
      { key: 'org-na2co3', label: '纯碱', latex: '\\mathrm{Na_2CO_3}', preview: '\\mathrm{Na_2CO_3}' },
      { key: 'org-nahco3', label: '小苏打', latex: '\\mathrm{NaHCO_3}', preview: '\\mathrm{NaHCO_3}' },
      { key: 'org-sucrose', label: '蔗糖', latex: '\\mathrm{C_{12}H_{22}O_{11}}', preview: '\\ce{C12H22O11}' },
      { key: 'org-cellulose', label: '纤维素', latex: '(\\mathrm{C_6H_{10}O_5})_n', preview: '\\ce{(C6H10O5)_n}' },
    ]
  },
  {
    key: 'biology',
    label: '生物',
    templates: [
      { key: 'bio-dna', label: 'DNA', latex: '\\mathrm{DNA}', preview: '\\mathrm{DNA}' },
      { key: 'bio-rna', label: 'RNA', latex: '\\mathrm{RNA}', preview: '\\mathrm{RNA}' },
      { key: 'bio-atp', label: 'ATP', latex: '\\mathrm{ATP}', preview: '\\mathrm{ATP}' },
      { key: 'bio-adp', label: 'ADP', latex: '\\mathrm{ADP}', preview: '\\mathrm{ADP}' },
      { key: 'bio-enzyme', label: '酶', latex: '\\mathrm{E}', preview: '\\mathrm{E}' },
      { key: 'bio-substrate', label: '底物', latex: '\\mathrm{S}', preview: '\\mathrm{S}' },
      { key: 'bio-product', label: '产物', latex: '\\mathrm{P}', preview: '\\mathrm{P}' },
      { key: 'bio-photosynthesis', label: '光合作用', latex: '6\\mathrm{CO_2}+6\\mathrm{H_2O}\\xrightarrow{\\text{光能}}\\mathrm{C_6H_{12}O_6}+6\\mathrm{O_2}', preview: '6\\mathrm{CO_2}+6\\mathrm{H_2O}' },
      { key: 'bio-respiration', label: '有氧呼吸', latex: '\\mathrm{C_6H_{12}O_6}+6\\mathrm{O_2}\\rightarrow 6\\mathrm{CO_2}+6\\mathrm{H_2O}', preview: '\\mathrm{C_6H_{12}O_6}+\\mathrm{O_2}' },
      { key: 'bio-fermentation', label: '发酵', latex: '\\mathrm{C_6H_{12}O_6}\\rightarrow 2\\mathrm{C_2H_5OH}+2\\mathrm{CO_2}', preview: '\\mathrm{C_6H_{12}O_6}' },
      { key: 'bio-mitosis', label: '有丝分裂', latex: '2n\\rightarrow 2n', preview: '2n' },
      { key: 'bio-meiosis', label: '减数分裂', latex: '2n\\rightarrow n', preview: '2n\\rightarrow n' },
      { key: 'bio-genotype', label: '基因型', latex: '\\mathrm{Aa}', preview: '\\mathrm{Aa}' },
      { key: 'bio-phenotype', label: '表现型', latex: '\\mathrm{A}_{-}', preview: '\\mathrm{A}_{-}' },
      { key: 'bio-hardy', label: '哈温平衡', latex: 'p^2+2pq+q^2=1', preview: 'p^2+2pq+q^2=1' },
      { key: 'bio-concentration', label: '浓度', latex: 'c=\\frac{n}{V}', preview: 'c=\\frac{n}{V}' },
      { key: 'bio-ph', label: 'pH', latex: '\\mathrm{pH}', preview: '\\mathrm{pH}' },
      { key: 'bio-arrow', label: '反应→', latex: '\\rightarrow', preview: '\\rightarrow' },
      { key: 'bio-reversible', label: '可逆⇌', latex: '\\rightleftharpoons', preview: '\\rightleftharpoons' },
      { key: 'bio-percent', label: '百分号', latex: '\\%', preview: '\\%' },
      { key: 'bio-delta', label: '变化量', latex: '\\Delta', preview: '\\Delta' },
      { key: 'bio-mu', label: '平均', latex: '\\mu', preview: '\\mu' },
      { key: 'bio-chlorophyll', label: '叶绿素', latex: '\\mathrm{C_{55}H_{72}MgN_4O_5}', preview: '\\mathrm{Mg}' },
      { key: 'bio-glucose', label: '葡萄糖', latex: '\\mathrm{C_6H_{12}O_6}', preview: '\\mathrm{C_6H_{12}O_6}' },
      { key: 'bio-amino', label: '氨基酸', latex: '\\mathrm{H_2N-CHR-COOH}', preview: '\\mathrm{NH_2}' },
      { key: 'bio-peptide', label: '肽键', latex: '\\mathrm{-CO-NH-}', preview: '\\mathrm{CO\\text{-}NH}' },
      { key: 'bio-oxygen', label: '氧气', latex: '\\mathrm{O_2}', preview: '\\mathrm{O_2}' },
      { key: 'bio-co2', label: '二氧化碳', latex: '\\mathrm{CO_2}', preview: '\\mathrm{CO_2}' },
      { key: 'bio-water', label: '水', latex: '\\mathrm{H_2O}', preview: '\\mathrm{H_2O}' },
      { key: 'bio-nadh', label: 'NADH', latex: '\\mathrm{NADH}', preview: '\\mathrm{NADH}' },
      { key: 'bio-nadp', label: 'NADPH', latex: '\\mathrm{NADPH}', preview: '\\mathrm{NADPH}' },
    ]
  },
  {
    key: 'common',
    label: '常用',
    templates: [
      { key: 'cm-frac', label: '分数', latex: '\\frac{#?}{#?}', preview: '\\frac{a}{b}' },
      { key: 'cm-sqrt', label: '根号', latex: '\\sqrt{#?}', preview: '\\sqrt{x}' },
      { key: 'cm-sup', label: '上标', latex: '^{#?}', preview: 'x^{2}' },
      { key: 'cm-sub', label: '下标', latex: '_{#?}', preview: 'x_{i}' },
      { key: 'cm-arrow-r', label: '→', latex: '\\rightarrow', preview: '\\rightarrow' },
      { key: 'cm-arrow-l', label: '←', latex: '\\leftarrow', preview: '\\leftarrow' },
      { key: 'cm-arrow-bi', label: '⇌', latex: '\\rightleftharpoons', preview: '\\rightleftharpoons' },
      { key: 'cm-degree', label: '度', latex: '^{\\circ}', preview: '90^{\\circ}' },
      { key: 'cm-percent', label: '%', latex: '\\%', preview: '\\%' },
      { key: 'cm-times', label: '×', latex: '\\times', preview: '\\times' },
      { key: 'cm-div', label: '÷', latex: '\\div', preview: '\\div' },
      { key: 'cm-pm', label: '±', latex: '\\pm', preview: '\\pm' },
      { key: 'cm-neq', label: '≠', latex: '\\neq', preview: '\\neq' },
      { key: 'cm-ge', label: '≥', latex: '\\ge', preview: '\\ge' },
      { key: 'cm-le', label: '≤', latex: '\\le', preview: '\\le' },
      { key: 'cm-approx', label: '≈', latex: '\\approx', preview: '\\approx' },
      { key: 'cm-infty', label: '∞', latex: '\\infty', preview: '\\infty' },
      { key: 'cm-pi', label: 'π', latex: '\\pi', preview: '\\pi' },
      { key: 'cm-alpha', label: 'α', latex: '\\alpha', preview: '\\alpha' },
      { key: 'cm-beta', label: 'β', latex: '\\beta', preview: '\\beta' },
      { key: 'cm-theta', label: 'θ', latex: '\\theta', preview: '\\theta' },
      { key: 'cm-lambda', label: 'λ', latex: '\\lambda', preview: '\\lambda' },
      { key: 'cm-Delta', label: 'Δ', latex: '\\Delta', preview: '\\Delta' },
      { key: 'cm-omega', label: 'ω', latex: '\\omega', preview: '\\omega' },
      { key: 'cm-vec', label: '向量', latex: '\\vec{#?}', preview: '\\vec{v}' },
      { key: 'cm-overline', label: '平均', latex: '\\overline{#?}', preview: '\\overline{x}' },
      { key: 'cm-ce', label: '化学式', latex: '\\ce{#?}', preview: '\\ce{H2O}' },
    ]
  },
]

/** Flat list for backward compatibility. */
export const QUICK_FORMULA_TEMPLATES = FORMULA_TEMPLATE_GROUPS.flatMap(g => g.templates)

/** Map school subject name to default formula tab key. */
const SUBJECT_CATEGORY_MAP = {
  '\u6570\u5b66': 'math',
  '\u7269\u7406': 'physics',
  '\u5316\u5b66': 'chemistry',
  '\u751f\u7269': 'biology',
  '\u5730\u7406': 'physics',
  '\u653f\u6cbb': 'common',
  '\u5386\u53f2': 'common',
  '\u8bed\u6587': 'common',
  '\u82f1\u8bed': 'common'
}

export function getFormulaCategoryForSubject(subjectName) {
  if (!subjectName) return 'math'
  const name = String(subjectName).trim()
  if (SUBJECT_CATEGORY_MAP[name]) return SUBJECT_CATEGORY_MAP[name]
  if (/物理|力学|电磁|光学/.test(name)) return 'physics'
  if (/化学|有机/.test(name)) return 'chemistry'
  if (/生物|生命/.test(name)) return 'biology'
  if (/数学|代数|几何/.test(name)) return 'math'
  return 'common'
}

function buildTemplateMenuCommand(mathField, template) {
  return {
    type: 'command',
    label: template.label,
    onMenuSelect: () => insertFormulaTemplate(mathField, template.latex)
  }
}

function buildSubjectFormulaSubmenu(mathField) {
  return {
    type: 'submenu',
    id: 'qb-subject-formulas',
    label: '\u5b66\u79d1\u516c\u5f0f',
    submenu: FORMULA_TEMPLATE_GROUPS.map(group => ({
      type: 'submenu',
      label: group.label,
      columnCount: 2,
      submenu: group.templates.map(t => buildTemplateMenuCommand(mathField, t))
    }))
  }
}

/** Extend MathLive context menu insert submenu with per-subject formula templates. */
export function applySubjectFormulaMenu(mathField) {
  if (!mathField || typeof mathField.menuItems === 'undefined') return false
  const baseItems = [...mathField.menuItems]
  const subjectMenu = buildSubjectFormulaSubmenu(mathField)
  const insertIdx = baseItems.findIndex(item => item && item.id === 'insert')

  if (insertIdx >= 0) {
    const insertItem = { ...baseItems[insertIdx] }
    const submenu = [...(insertItem.submenu || [])]
    submenu.push({ type: 'divider' })
    submenu.push(subjectMenu)
    insertItem.submenu = submenu
    baseItems[insertIdx] = insertItem
  } else {
    const cutIdx = baseItems.findIndex(item => item && item.id === 'cut')
    const pos = cutIdx >= 0 ? cutIdx : 0
    baseItems.splice(pos, 0, { type: 'divider' }, subjectMenu)
  }

  mathField.menuItems = baseItems
  return true
}

const ZH_CN_MENU_STRINGS = {
  'tooltip.menu': '菜单',
  'tooltip.cut to clipboard': '剪切到剪贴板',
  'tooltip.paste from clipboard': '从剪贴板粘贴',
  'tooltip.copy to clipboard': '复制到剪贴板',
  'menu.insert matrix': '插入矩阵',
  'menu.borders': '边框分隔',
  'menu.mode': '模式',
  'menu.mode-math': '数学',
  'menu.mode-text': '文本',
  'menu.mode-latex': 'LaTeX',
  'menu.font-style': '字体样式',
  'menu.accent': '重音',
  'menu.decoration': '装饰',
  'menu.color': '颜色',
  'menu.background-color': '背景',
  'menu.cut': '剪切',
  'menu.copy': '复制',
  'menu.copy-as-latex': '复制为 LaTeX',
  'menu.paste': '粘贴',
  'menu.select-all': '全选',
  'menu.insert': '插入',
  'menu.insert.abs': '绝对值',
  'menu.insert.abs-template': '\\left|x\\right|',
  'menu.insert.nth-root': 'n<sup>次</sup>根',
  'menu.insert.nth-root-template': '\\sqrt[n]{x}',
  'menu.insert.log-base': '底数为 a 的对数',
  'menu.insert.log-base-template': '\\log_a(x)',
  'menu.insert.heading-calculus': '微积分',
  'menu.insert.derivative': '导数',
  'menu.insert.derivative-template': '\\dfrac{\\mathrm{d}}{\\mathrm{d}x}f(x)\\bigm|_{x=a}',
  'menu.insert.nth-derivative': 'n<sup>次</sup>导数',
  'menu.insert.nth-derivative-template': '\\dfrac{\\mathrm{d}^n}{\\mathrm{d}x^n}f(x)\\bigm|_{x=a}',
  'menu.insert.integral': '定积分',
  'menu.insert.integral-template': '$\\int_a^b f(x)\\,\\mathrm{d}x$',
  'menu.insert.sum': '求和',
  'menu.insert.sum-template': '$\\sum_{i=1}^n x_i$',
  'menu.insert.product': '连乘',
  'menu.insert.product-template': '\\prod_{i=1}^n x_i',
  'menu.insert.heading-complex-numbers': '复数',
  'menu.insert.modulus': '模',
  'menu.insert.modulus-template': '\\lvert z \\rvert',
  'menu.insert.argument': '幅角',
  'menu.insert.argument-template': '\\arg(z)',
  'menu.insert.real-part': '实部',
  'menu.insert.real-part-template': '\\Re(z)',
  'menu.insert.imaginary-part': '虚部',
  'menu.insert.imaginary-part-template': '\\Im(z)',
  'menu.insert.conjugate': '共轭',
  'menu.insert.conjugate-template': '\\overline{z}',
}

export async function configureMathLiveOnce() {
  if (configured) return null
  const mod = await import('mathlive')
  const MathfieldElement = mod.MathfieldElement
  if (!MathfieldElement) return null
  const builtin = (MathfieldElement.strings && MathfieldElement.strings['zh-cn']) || {}
  MathfieldElement.strings = {
    ...(MathfieldElement.strings || {}),
    'zh-cn': { ...builtin, ...ZH_CN_MENU_STRINGS }
  }
  MathfieldElement.locale = 'zh-cn'
  configured = true
  return MathfieldElement
}

export function insertFormulaTemplate(mathField, latex) {
  if (!mathField || !latex) return false
  if (typeof mathField.insert === 'function') {
    return mathField.insert(latex, { selectionMode: 'placeholder' })
  }
  if (typeof mathField.executeCommand === 'function') {
    return mathField.executeCommand(['insert', latex, { selectionMode: 'placeholder' }])
  }
  return false
}

export function applyMathFieldLocale(el) {
  if (el && el.setAttribute) {
    el.setAttribute('locale', 'zh-cn')
  }
}

