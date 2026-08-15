const fs = require('fs')
const path = require('path')
const file = path.join(__dirname, '../src/views/education/question-bank/paper/preview.vue')
let text = fs.readFileSync(file, 'utf8')

const pairs = [
  ['label="\\u5361\\u7247\\u7c7b\\u578b"', 'label="��Ƭ����"'],
  ['>\\u5b66\\u751f\\u586b\\u6d62\\u5361<', '>ѧ����Ϳ��<'],
  ['>\\u6559\\u5e08\\u53c2\\u8003\\u7248<', '>��ʦ�ο���<'],
  ['label="\\u7248\\u9762\\u98ce\\u683c"', 'label="������"'],
  ['>\\u6807\\u51c6<', '>��׼<'],
  ['>\\u7d27\\u51d1<', '>����<'],
  ['label="\\u5305\\u542b\\u533a\\u57df"', 'label="��������"'],
  ['>\\u5ba2\\u89c2\\u9898\\u586b\\u6d62<', '>�͹�����Ϳ<'],
  ['>\\u586b\\u7a7a\\u9898<', '>�����<'],
  ['>\\u4e3b\\u89c2\\u9898\\u4f5c\\u7b54<', '>����������<'],
  ['label="\\u663e\\u793a\\u5206\\u503c"', 'label="��ʾ��ֵ"'],
  ['label="\\u586b\\u6d62\\u5217\\u6570"', 'label="��Ϳ����"'],
  ['label="\\u8003\\u53f7\\u586b\\u6d62"', 'label="������Ϳ"'],
  ['<span>\\u5b9e\\u65f6\\u9884\\u89c8</span>', '<span>ʵʱԤ��</span>'],
  ['@click="refreshAnswerSheetPreview">\\u5237\\u65b0</el-button>', '@click="refreshAnswerSheetPreview">ˢ��</el-button>'],
  ['description="\\u9884\\u89c8\\u52a0\\u8f7d\\u4e2d..."', 'description="Ԥ��������..."'],
  ['@click="answerSheetOpen = false">\\u53d6\\u6d88</el-button>', '@click="answerSheetOpen = false">ȡ��</el-button>'],
  ['@click="printAnswerSheet">\\u6253\\u5370</el-button>', '@click="printAnswerSheet">��ӡ</el-button>'],
  ['@click="confirmAnswerSheetExport">\\u4e0b\\u8f7d PDF</el-button>', '@click="confirmAnswerSheetExport">���� PDF</el-button>']
]
pairs.forEach(([from, to]) => {
  text = text.split(from).join(to)
})
fs.writeFileSync(file, text, 'utf8')
console.log('fixed workshop dialog Chinese labels')
