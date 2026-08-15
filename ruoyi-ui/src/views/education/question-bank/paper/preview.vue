<template>
  <div class="app-container paper-compose-page" :class="{ 'paper-compose-page--portal': isPortal }">
    <div class="compose-topbar">
      <div class="compose-breadcrumb">
        <span class="crumb-link" @click="goBack">{{ breadcrumbParentLabel }}</span>
        <i class="el-icon-arrow-right" />
        <span class="crumb-current">{{ breadcrumbCurrentLabel }}</span>
      </div>
      <a class="compose-help" href="javascript:;" @click.prevent="showHelp">组卷中心怎么用？</a>
    </div>

    <el-row :gutter="16" class="compose-layout">
      <el-col :span="18">
        <div v-loading="detailLoading" class="paper-canvas-wrap">
          <div v-if="!sortedItems.length" class="paper-empty">
            <el-empty description="试题栏为空，请先加入试题" />
          </div>
          <div v-else class="paper-canvas">
            <div class="paper-title-row">
              <el-input
                v-model="form.paperTitle"
                class="paper-title-input"
                placeholder="请输入试卷标题"
                @blur="onTitleBlur"
              />
            </div>
            <div class="paper-meta-line">
              <span class="meta-field">学校<u class="meta-blank" /></span>
              <span class="meta-field">姓名<u class="meta-blank" /></span>
              <span class="meta-field">班级<u class="meta-blank" /></span>
              <span class="meta-field">考号<u class="meta-blank wide" /></span>
            </div>

            <div v-for="vol in paperVolumes" :key="vol.key" class="paper-volume">
              <div v-if="vol.showTitle" class="volume-title">{{ vol.title }}</div>
              <div v-for="section in vol.sections" :key="section.key" class="paper-section">
                <div v-if="section.title" class="section-title">{{ section.title }}</div>
                <div
                  v-for="q in section.items"
                  :key="q.questionId"
                  :ref="'q-' + q.questionId"
                  class="paper-question"
                  :class="{
                    'is-active': activeQuestionId === q.questionId,
                    'is-answer-expanded': answerExpanded[q.questionId]
                  }"
                  @mouseenter="activeQuestionId = q.questionId"
                  @mouseleave="onQuestionLeave"
                  @click="onQuestionClick(q, $event)"
                >
                  <div v-show="activeQuestionId === q.questionId" class="question-toolbar">
                    <button type="button" class="tb-btn" @click.stop="openSimilarReplace(q)">换相似题</button>
                    <button type="button" class="tb-btn" @click.stop="openReplace(q)">换题</button>
                    <button type="button" class="tb-btn" @click.stop="openFeedback(q)">纠错</button>
                    <button type="button" class="tb-btn" @click.stop="openDetail(q)">详情</button>
                    <button type="button" class="tb-btn" @click.stop="openAnswerArea(q)">插入作答区</button>
                    <span class="tb-score">
                      <el-input-number
                        :value="q.scoreValue"
                        :min="0"
                        :max="100"
                        :step="1"
                        size="mini"
                        controls-position="right"
                        @click.native.stop
                        @change="val => setScore(q.questionId, val)"
                      />
                      <em>分值</em>
                    </span>
                    <button type="button" class="tb-icon" title="上移" @click.stop="moveQuestion(q.questionId, -1)">
                      <i class="el-icon-top" />
                    </button>
                    <button type="button" class="tb-icon" title="下移" @click.stop="moveQuestion(q.questionId, 1)">
                      <i class="el-icon-bottom" />
                    </button>
                    <button type="button" class="tb-icon danger" title="删除" @click.stop="removeQuestion(q.questionId)">
                      <i class="el-icon-delete" />
                    </button>
                  </div>
                  <div class="question-stem">
                    <span class="q-no">{{ q.globalNo }}.</span>
                    <qb-formula-text class="q-text" :text="displayContent(q)" />
                  </div>
                  <div v-if="imageUrls(q).length" class="question-images">
                    <el-image
                      v-for="(url, i) in imageUrls(q)"
                      :key="i"
                      :src="resolveImageUrl(url)"
                      :preview-src-list="previewSrcList(q)"
                      fit="contain"
                      class="question-image"
                    />
                  </div>
                  <ul v-if="optionItems(q).length" class="question-options">
                    <li v-for="opt in optionItems(q)" :key="opt.label" class="option-item">
                      <span class="option-label">{{ opt.label }}.</span>
                      <qb-formula-text class="option-text" :text="opt.text" />
                    </li>
                  </ul>
                  <div v-if="getAnswerArea(q.questionId)" class="answer-area-block">
                    <div v-if="getAnswerArea(q.questionId).style === 'blank'" class="answer-area-blank" :style="blankAreaStyle(q.questionId)" />
                    <div v-else class="answer-area-ruled">
                      <div v-for="n in getAnswerArea(q.questionId).lines" :key="n" class="answer-area-line" />
                    </div>
                  </div>
                  <div v-if="answerExpanded[q.questionId]" class="answer-expand-block">
                    <div v-if="teacherAnswer(q)" class="teacher-row">
                      <span class="teacher-tag">【答案】</span>
                      <qb-formula-text :text="teacherAnswer(q)" />
                    </div>
                    <div v-if="teacherAnalysis(q)" class="teacher-row">
                      <span class="teacher-tag">【详解】</span>
                      <qb-formula-text :text="teacherAnalysis(q)" />
                    </div>
                    <div v-if="!teacherAnswer(q) && !teacherAnalysis(q)" class="teacher-row teacher-empty">暂无答案或解析</div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </el-col>

      <el-col :span="6">
        <div class="sidebar-stack">
          <div class="sidebar-card stats-card">
            <div class="stat-item"><label>题量</label><b>{{ basketCount }}</b></div>
            <div class="stat-item"><label>难度</label><b>{{ difficultyLabel }}</b></div>
            <div class="stat-item"><label>分值</label><b>{{ basketTotalScore }}</b></div>
          </div>

          <div class="sidebar-card score-quick-card">
            <div class="card-head"><span>快速配分</span></div>
            <div class="score-quick-row">
              <span class="score-quick-label">目标总分</span>
              <el-input-number v-model="targetTotalScore" :min="1" :max="300" size="mini" controls-position="right" />
              <el-button type="primary" size="mini" plain @click="distributeScoreEvenly">平均分配</el-button>
            </div>
            <div v-for="row in scoreTypeRows" :key="row.type" class="score-type-row">
              <span class="score-type-label">{{ row.label }}<em>{{ row.count }}题</em></span>
              <div class="score-type-input">
                <el-input-number
                  v-model="row.scorePerItem"
                  :min="0"
                  :max="100"
                  size="mini"
                  controls-position="right"
                  @change="val => applyTypeScore(row.type, val)"
                />
                <span class="score-type-unit">分/题</span>
              </div>
            </div>
          </div>

          <div class="sidebar-card action-card">
            <div class="action-grid">
              <a v-for="act in sideActions" :key="act.key" href="javascript:;" class="action-link" @click.prevent="act.handler">
                <i :class="act.icon" /><span>{{ act.label }}</span>
              </a>
            </div>
            <div class="export-options">
              <div class="export-row">
                <span class="export-label">版本</span>
                <el-radio-group v-model="form.exportMode" size="mini" class="export-radios" @change="onExportModeChange">
                  <el-radio label="student">学生版</el-radio>
                  <el-radio label="teacher">教师版</el-radio>
                </el-radio-group>
              </div>
              <div v-if="form.exportMode === 'teacher'" class="export-row">
                <span class="export-label">答案</span>
                <el-radio-group v-model="form.answerLayout" size="mini" class="export-radios">
                  <el-radio label="inline">题后</el-radio>
                  <el-radio label="end">卷末</el-radio>
                </el-radio-group>
              </div>
              <div class="export-row">
                <span class="export-label">格式</span>
                <el-radio-group v-model="exportFormat" size="mini" class="export-radios">
                  <el-radio label="pdf">PDF</el-radio>
                  <el-radio label="docx">Word</el-radio>
                </el-radio-group>
              </div>
              <div class="export-row">
                <span class="export-label">版面</span>
                <el-radio-group v-model="pageLayout" size="mini" class="export-radios" @change="onPageLayoutChange">
                  <el-radio label="A4">A4</el-radio>
                  <el-radio label="A3">A3</el-radio>
                </el-radio-group>
              </div>
            </div>
            <el-button type="primary" class="download-btn" :loading="exportLoading" :disabled="!canPreview" @click="exportPaper">
              下载试卷
            </el-button>
          </div>

          <div class="sidebar-card">
            <div class="card-head">
              <span>试卷模板</span>
              <el-button type="text" size="mini" @click="templateSettingsOpen = true">设置</el-button>
            </div>
            <el-radio-group v-model="paperTemplate" class="template-radios" @change="onTemplateChange">
              <el-radio label="homework">作业</el-radio>
              <el-radio label="test">测试</el-radio>
              <el-radio label="exam">考试</el-radio>
            </el-radio-group>
          </div>

          <div class="sidebar-card">
            <div class="card-head"><span>分组与排序</span></div>
            <div class="group-tabs">
              <span
                v-for="tab in groupTabs"
                :key="tab.key"
                class="group-tab"
                :class="{ active: groupTab === tab.key }"
                @click="setGroupTab(tab.key)"
              >{{ tab.label }}</span>
            </div>
            <el-radio-group v-model="orderRadio" class="order-radios" @change="syncSortMode">
              <el-radio label="order">加入顺序</el-radio>
              <el-radio label="difficulty">由易到难</el-radio>
            </el-radio-group>
            <div class="nav-map">
              <div v-for="vol in paperVolumes" :key="'nav-' + vol.key" class="nav-volume">
                <div v-if="vol.showTitle" class="nav-volume-title">{{ vol.title }}</div>
                <div v-for="section in vol.sections" :key="'nav-' + section.key" class="nav-section">
                  <div v-if="section.shortTitle" class="nav-section-title">{{ section.shortTitle }}</div>
                  <div class="nav-boxes">
                    <span
                      v-for="q in section.items"
                      :key="'box-' + q.questionId"
                      class="nav-box"
                      :class="{ active: activeQuestionId === q.questionId }"
                      @click="scrollToQuestion(q.questionId)"
                    >{{ q.globalNo }}</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </el-col>
    </el-row>

    <el-dialog title="试卷模板设置" :visible.sync="templateSettingsOpen" width="420px" append-to-body>
      <el-form label-width="72px" size="small">
        <el-form-item label="学校">
          <el-input v-model="form.header.schoolName" placeholder="东陆中学" />
        </el-form-item>
        <el-form-item label="科目">
          <el-input v-model="form.header.subjectName" placeholder="物理" />
        </el-form-item>
        <el-form-item label="时长">
          <el-input v-model="form.header.duration" placeholder="90 分钟" />
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button type="primary" size="small" @click="templateSettingsOpen = false">确定</el-button>
      </div>
    </el-dialog>

    <paper-replace-question-dialog
      v-model="replaceOpen"
      :current-question="replaceQuestion"
      :exclude-ids="basketQuestionIds"
      :initial-tab="replaceInitialTab"
      @replace="onReplaceQuestion"
    />
    <paper-question-feedback-dialog
      v-model="feedbackOpen"
      :question="feedbackQuestion"
      :paper-title="form.paperTitle"
    />
    <paper-answer-area-dialog
      v-model="answerAreaOpen"
      :question="answerAreaQuestion"
      :existing="answerAreaQuestion ? getAnswerArea(answerAreaQuestion.questionId) : null"
      @confirm="onAnswerAreaConfirm"
      @clear="onAnswerAreaClear"
    />

    <el-dialog :title="'试卷分析'" :visible.sync="analysisOpen" width="520px" append-to-body class="paper-analysis-dialog">
      <div v-if="paperAnalysisStats" class="analysis-body">
        <div class="analysis-summary">
          <div class="analysis-stat"><label>题量</label><b>{{ paperAnalysisStats.count }}</b></div>
          <div class="analysis-stat"><label>总分</label><b>{{ paperAnalysisStats.totalScore }}</b></div>
          <div class="analysis-stat"><label>平均难度</label><b>{{ paperAnalysisStats.difficultyLabel }}</b></div>
          <div class="analysis-stat"><label>考试时长</label><b>{{ paperAnalysisStats.duration }}</b></div>
        </div>
        <div class="analysis-block">
          <div class="analysis-block-title">题型分布</div>
          <el-table :data="paperAnalysisStats.typeRows" size="mini" border>
            <el-table-column prop="label" :label="'题型'" />
            <el-table-column prop="count" :label="'题数'" width="72" align="center" />
            <el-table-column prop="score" :label="'分值'" width="72" align="center" />
            <el-table-column prop="ratio" :label="'占比'" width="80" align="center">
              <template slot-scope="scope">{{ scope.row.ratio }}%</template>
            </el-table-column>
          </el-table>
        </div>
        <div class="analysis-block">
          <div class="analysis-block-title">难度分布</div>
          <div class="diff-bars">
            <div v-for="row in paperAnalysisStats.diffRows" :key="row.key" class="diff-row">
              <span class="diff-label">{{ row.label }}</span>
              <div class="diff-bar-wrap"><div class="diff-bar" :style="{ width: row.percent + '%' }" /></div>
              <span class="diff-count">{{ row.count }}</span>
            </div>
          </div>
        </div>
      </div>
      <div slot="footer"><el-button size="small" @click="analysisOpen = false">关闭</el-button></div>
    </el-dialog>

    <el-dialog :title="'保存成功'" :visible.sync="saveDialogOpen" width="420px" append-to-body>
      <p class="save-dialog-hint">试卷已保存，可在「我的试卷」中继续编辑。</p>
      <div v-if="saveDialogInfo" class="save-dialog-meta">
        <div><label>试卷标题</label>{{ saveDialogInfo.title }}</div>
        <div><label>题量</label>{{ saveDialogInfo.count }} 题</div>
        <div><label>总分</label>{{ saveDialogInfo.score }} 分</div>
        <div><label>保存时间</label>{{ saveDialogInfo.savedAtText }}</div>
      </div>
      <div slot="footer"><el-button type="primary" size="small" @click="onSaveDialogClose">确定</el-button></div>
    </el-dialog>

    <el-dialog :title="'分享试卷'" :visible.sync="shareDialogOpen" width="480px" append-to-body>
      <p class="share-dialog-hint">{{ shareLinkHint }}</p>
      <el-input v-model="shareLink" readonly size="small">
        <el-button slot="append" @click="copyShareLink">复制链接</el-button>
      </el-input>
      <div slot="footer"><el-button size="small" @click="shareDialogOpen = false">关闭</el-button></div>
    </el-dialog>

    
    <el-dialog
      :title="answerSheetWorkshopMode ? '制作答题卡' : '答题卡设置'"
      :visible.sync="answerSheetOpen"
      width="960px"
      append-to-body
      class="answer-sheet-workshop-dialog"
      @open="onAnswerSheetDialogOpen"
    >
      <div class="as-workshop">
        <div class="as-workshop-left">
          <div class="as-stats-bar">
            <span>共 {{ answerSheetStats.total }} 题</span>
            <span>客观 {{ answerSheetStats.objective }}</span>
            <span>填空 {{ answerSheetStats.fill }}</span>
            <span>主观 {{ answerSheetStats.subjective }}</span>
            <span>满分 {{ answerSheetStats.totalScore }}</span>
          </div>
          <el-form label-width="96px" size="small">
            <el-form-item label="卡片类型">
              <el-radio-group v-model="answerSheetOptions.sheetMode">
                <el-radio label="student">学生填涂卡</el-radio>
                <el-radio label="teacher">教师参考版</el-radio>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="版面风格">
              <el-radio-group v-model="answerSheetOptions.style">
                <el-radio label="standard">标准</el-radio>
                <el-radio label="compact">紧凑</el-radio>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="包含区域">
              <el-checkbox v-model="answerSheetOptions.includeObjective">客观题填涂</el-checkbox>
              <el-checkbox v-model="answerSheetOptions.includeFill">填空题</el-checkbox>
              <el-checkbox v-model="answerSheetOptions.includeSubjective">主观题作答</el-checkbox>
            </el-form-item>
            <el-form-item label="显示分值">
              <el-switch v-model="answerSheetOptions.showScore" />
            </el-form-item>
            <el-form-item label="填涂列数">
              <el-radio-group v-model="answerSheetOptions.choicePerRow">
                <el-radio :label="5">5 题/行</el-radio>
                <el-radio :label="10">10 题/行</el-radio>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="考号填涂">
              <el-switch v-model="answerSheetOptions.showExamNumber" />
            </el-form-item>
          </el-form>
        </div>
        <div v-loading="answerSheetPreviewLoading" class="as-workshop-right">
          <div class="as-preview-toolbar">
            <span>实时预览</span>
            <el-button type="text" size="mini" icon="el-icon-refresh" @click="refreshAnswerSheetPreview">刷新</el-button>
          </div>
          <div class="as-preview-scroll">
            <div v-if="answerSheetPreviewHtml" class="as-preview-paper" v-html="answerSheetPreviewHtml" />
            <el-empty v-else :description="'预览加载中...'" />
          </div>
        </div>
      </div>
      <div slot="footer">
        <el-button size="small" @click="answerSheetOpen = false">取消</el-button>
        <el-button size="small" icon="el-icon-printer" :loading="sheetLoading" @click="printAnswerSheet">打印</el-button>
        <el-button size="small" icon="el-icon-document" :loading="sheetLoading" @click="confirmAnswerSheetDocxExport">下载 Word</el-button>
        <el-button type="primary" size="small" icon="el-icon-download" :loading="sheetLoading" @click="confirmAnswerSheetExport">下载 PDF</el-button>
      </div>
    </el-dialog>

    <paper-question-detail-dialog v-model="detailOpen" :question-id="detailQuestionId" />
    <pay-dialog ref="payDialog" />
  </div>
</template>

<script>
import { mapGetters } from 'vuex'
import { getQuestion } from '@/api/education/question'
import { stripLeadingQuestionNo } from '@/utils/questionContent'
import { loadPaperDraft, savePaperDraft, loadPaperShare } from '@/utils/questionBasketPrefs'
import {
  buildPaperShareUrl,
  createLocalPaperShare,
  createServerPaperShare,
  isServerShareId
} from '@/utils/paperShare'
import { exportPaperClient } from '@/utils/paperExportClient'
import {
  exportAnswerSheetClient,
  printAnswerSheetClient,
  buildAnswerSheetPreviewHtml,
  getAnswerSheetStats
} from '@/utils/paperAnswerSheetExport'
import { exportAnswerSheetDocxClient } from '@/utils/paperAnswerSheetExportDocx'
import { saveMyPaper, getPaperShare, publishSchoolPaper } from '@/api/education/paper'
import { goPortalLogin } from '@/utils/portalLogin'
import { isPortalPath } from '@/constants/routes'
import PaperReplaceQuestionDialog from './components/PaperReplaceQuestionDialog'
import PaperQuestionFeedbackDialog from './components/PaperQuestionFeedbackDialog'
import PaperAnswerAreaDialog from './components/PaperAnswerAreaDialog'
import PaperQuestionDetailDialog from './components/PaperQuestionDetailDialog'
import PayDialog from '@/components/PayDialog'
import { checkPayAccess, PAY_BIZ } from '@/api/education/pay'
import { parseQuestionOption } from '@/utils/questionOptions'
import { formatChoiceAnswer } from '@/utils/questionAnswer'
import dynamicQuestionTypes from '@/mixins/dynamicQuestionTypes'
import {
  getQuestionTypeOrder,
  getQuestionTypeLabel,
  groupItemsByQuestionType,
  isPaperChoiceVolumeType
} from '@/utils/questionTypes'

const SECTION_NUMS = ['一', '二', '三', '四', '五', '六', '七', '八', '九', '十']
const TEMPLATE_SUFFIX = { homework: '作业', test: '测试', exam: '考试' }

export default {
  name: 'QuestionPaperPreview',
  mixins: [dynamicQuestionTypes],
  props: {
    portalMode: {
      type: Boolean,
      default: false
    }
  },
  components: {
    PaperReplaceQuestionDialog,
    PaperQuestionFeedbackDialog,
    PaperAnswerAreaDialog,
    PaperQuestionDetailDialog,
    PayDialog
  },
  data() {
    return {
      loading: false,
      exportLoading: false,
      exportBizRef: '',
      exportFormat: 'pdf',
      pageLayout: 'A4',
      detailLoading: false,
      activeQuestionId: null,
      replaceOpen: false,
      replaceInitialTab: 'search',
      replaceQuestion: null,
      feedbackOpen: false,
      feedbackQuestion: null,
      answerAreaOpen: false,
      answerAreaQuestion: null,
      answerAreas: {},
      answerExpanded: {},
      groupTab: 'type',
      orderRadio: 'difficulty',
      paperTemplate: 'homework',
      templateSettingsOpen: false,
      detailOpen: false,
      detailQuestionId: null,
      detailMap: {},
      form: {
        paperTitle: '',
        header: {
          schoolName: '',
          examTitle: '',
          subjectName: '',
          duration: '90 分钟'
        },
        templateCode: 'A4_1COL',
        sortMode: 'TYPE_THEN_DIFFICULTY',
        exportMode: 'student',
        answerLayout: 'inline',
        exportConfig: {
          fontSizePt: 12,
          marginMm: 20,
          showScore: true,
          watermark: ''
        }
      },
      groupTabs: [
        { key: 'type', label: '按题型' },
        { key: 'order', label: '按加入顺序' }
      ],
      sideActions: [],
      analysisOpen: false,
      saveDialogOpen: false,
      saveDialogInfo: null,
      _suppressBasketEmptyWarn: false,
      _leavingCompose: false,
      shareDialogOpen: false,
      shareLink: '',
      shareLinkMode: 'server',
      sheetLoading: false,
      savedPaperId: null,
      answerSheetOpen: false,
      answerSheetWorkshopMode: false,
      answerSheetPreviewHtml: '',
      answerSheetPreviewLoading: false,
      answerSheetOptions: {
        showScore: true,
        choicePerRow: 5,
        judgePerRow: 10,
        showExamNumber: true,
        includeObjective: true,
        includeFill: true,
        includeSubjective: true,
        sheetMode: 'student',
        style: 'standard'
      },
      targetTotalScore: 100,
      scoreTypeDraft: {}
    }
  },
  computed: {
    ...mapGetters(['questionBasketItems', 'questionBasketCount', 'questionBasketTotalScore', 'token']),
    isPortal() {
      if (this.portalMode) return true
      const path = this.$route && this.$route.path
      return path && isPortalPath(path)
    },
    breadcrumbParentLabel() {
      return this.isPortal ? '智能组卷' : '题库管理'
    },
    breadcrumbCurrentLabel() {
      return this.isPortal ? '组卷预览' : '组卷中心'
    },
    basketCount() {
      return this.questionBasketCount
    },
    basketTotalScore() {
      if (this.questionBasketTotalScore != null) {
        return this.questionBasketTotalScore
      }
      return this.questionBasketItems.reduce(
        (sum, i) => sum + (Number(i.scoreValue) || 0),
        0
      )
    },
    canPreview() {
      return this.questionBasketItems.length > 0 && this.basketTotalScore > 0
    },
    shareLinkHint() {
      if (this.shareLinkMode === 'local') {
        return '本机临时链接，仅当前浏览器可打开，换设备无效'
      }
      return '复制链接后可在任意设备浏览器打开（有效期 30 天）'
    },
    sharePreviewPath() {
      return this.isPortal
        ? '/paper/preview'
        : '/admin/question-bank-center/question-bank/paper/preview'
    },
    showTeacherBlock() {
      return this.form.exportMode === 'teacher'
    },
    difficultyLabel() {
      const items = this.questionBasketItems
      if (!items.length) return '-'
      const avg = items.reduce((s, i) => s + (Number(i.difficulty) || 0), 0) / items.length
      if (avg <= 0.35) return '容易'
      if (avg <= 0.65) return '中等'
      return '较难'
    },
    sortedItems() {
      return this.sortBasketItems(this.questionBasketItems)
    },
    paperVolumes() {
      const items = this.sortedItems
      if (!items.length) return []
      if (this.groupTab === 'order') {
        let globalNo = 0
        const flatItems = items.map(item => {
          globalNo += 1
          return { ...item, globalNo }
        })
        return [{
          key: 'flat',
          title: '',
          showTitle: false,
          sections: [{ key: 'flat', title: '', shortTitle: '全部题目', items: flatItems }]
        }]
      }
      const choiceItems = []
      const otherItems = []
      items.forEach(item => {
        if (isPaperChoiceVolumeType(item.questionType)) {
          choiceItems.push(item)
        } else {
          otherItems.push(item)
        }
      })
      const volumes = []
      let globalNo = 0
      const buildSections = (list, startIdx) => {
        const typeGroups = groupItemsByQuestionType(list)
        const sections = []
        let sectionIdx = startIdx
        typeGroups.forEach(group => {
          const sectionScore = group.items.reduce((s, i) => s + (Number(i.scoreValue) || 0), 0)
          const numeral = SECTION_NUMS[sectionIdx] || String(sectionIdx + 1)
          const perScore = group.items[0].scoreValue
          const sameScore = group.items.every(i => Number(i.scoreValue) === Number(perScore))
          let title
          if (sameScore) {
            title = `${numeral}、${group.label}（每题${perScore}分，共${sectionScore}分）`
          } else {
            title = `${numeral}、${group.label}（共${sectionScore}分）`
          }
          const sectionItems = group.items.map(item => {
            globalNo += 1
            return { ...item, globalNo }
          })
          sections.push({
            key: group.type,
            title,
            shortTitle: `${numeral}、${group.label}`,
            items: sectionItems
          })
          sectionIdx += 1
        })
        return sections
      }
      if (choiceItems.length) {
        volumes.push({
          key: 'vol1',
          title: '第I卷 选择题',
          showTitle: otherItems.length > 0 || this.groupTab !== 'order',
          sections: buildSections(choiceItems, 0)
        })
      }
      if (otherItems.length) {
        volumes.push({
          key: 'vol2',
          title: '第II卷 非选择题',
          showTitle: choiceItems.length > 0 || this.groupTab !== 'order',
          sections: buildSections(otherItems, choiceItems.length ? this.countSections(choiceItems) : 0)
        })
      }
      if (!choiceItems.length && !otherItems.length) {
        return []
      }
      if (!choiceItems.length && otherItems.length && volumes.length === 1) {
        volumes[0].showTitle = this.groupTab !== 'order'
      }
      return volumes
    },
    paperAnalysisStats() {
      const items = this.sortedItems
      if (!items.length) return null
      const totalScore = this.basketTotalScore
      const typeMap = {}
      const diffMap = { easy: 0, medium: 0, hard: 0 }
      items.forEach(item => {
        const type = item.questionType || 'short'
        const label = this.typeLabel(type)
        if (!typeMap[type]) typeMap[type] = { label, count: 0, score: 0 }
        typeMap[type].count += 1
        typeMap[type].score += Number(item.scoreValue) || 0
        const d = Number(item.difficulty) || 0
        if (d <= 0.35) diffMap.easy += 1
        else if (d <= 0.65) diffMap.medium += 1
        else diffMap.hard += 1
      })
      const typeRows = Object.values(typeMap)
        .map(row => ({
          ...row,
          ratio: totalScore > 0 ? Math.round((row.score / totalScore) * 100) : 0
        }))
        .sort((a, b) => b.count - a.count)
      const total = items.length
      const diffRows = [
        { key: 'easy', label: '容易', count: diffMap.easy, percent: total ? Math.round((diffMap.easy / total) * 100) : 0 },
        { key: 'medium', label: '中等', count: diffMap.medium, percent: total ? Math.round((diffMap.medium / total) * 100) : 0 },
        { key: 'hard', label: '较难', count: diffMap.hard, percent: total ? Math.round((diffMap.hard / total) * 100) : 0 }
      ]
      return {
        count: items.length,
        totalScore,
        difficultyLabel: this.difficultyLabel,
        duration: this.form.header.duration || '90 分钟',
        typeRows,
        diffRows
      }
    },
    answerSheetStats() {
      return getAnswerSheetStats(this)
    },
    basketQuestionIds() {
      return this.questionBasketItems.map(i => i.questionId)
    },
    scoreTypeRows() {
      return groupItemsByQuestionType(this.questionBasketItems).map(g => {
        const first = g.items[0]
        const scorePerItem = this.scoreTypeDraft[g.type] != null
          ? this.scoreTypeDraft[g.type]
          : (first && first.scoreValue != null ? Number(first.scoreValue) : 5)
        return {
          type: g.type,
          label: g.label,
          count: g.items.length,
          scorePerItem
        }
      })
    }
  },
  watch: {
    answerSheetOptions: {
      deep: true,
      handler() {
        if (!this.answerSheetOpen) return
        clearTimeout(this._answerSheetPreviewTimer)
        this._answerSheetPreviewTimer = setTimeout(() => this.refreshAnswerSheetPreview(), 280)
      }
    },
    questionBasketItems: {
      handler(val) {
        if (!val.length) {
          if (this._suppressBasketEmptyWarn) {
            this._suppressBasketEmptyWarn = false
            return
          }
          this.$modal.msgWarning('试题栏已清空')
        }
        if (this.form.exportMode === 'teacher') {
          this.ensureTeacherDetails()
        }
      },
      deep: true
    },
    'form.exportMode'(val) {
      if (val === 'teacher') {
        this.ensureTeacherDetails()
      }
    }
  },
  beforeRouteLeave(to, from, next) {
    if (this._leavingCompose) {
      next()
      return
    }
    this.handleComposeLeave(next)
  },
  created() {
    this.exportBizRef = this.buildExportBizRef()
    const actions = [
      { key: 'analysis', label: '试卷分析', icon: 'el-icon-data-analysis', handler: () => this.openAnalysis() },
      { key: 'save', label: '保存试卷', icon: 'el-icon-folder-checked', handler: () => this.confirmSaveDraft() },
      { key: 'sheet', label: '下载答题卡', icon: 'el-icon-tickets', handler: () => this.downloadAnswerSheetQuick() },
      { key: 'makeSheet', label: '制作答题卡', icon: 'el-icon-edit-outline', handler: () => this.openAnswerSheetWorkshop() },
      { key: 'share', label: '分享试卷', icon: 'el-icon-share', handler: () => this.sharePaper() },
      { key: 'practice', label: '在线练习', icon: 'el-icon-video-play', handler: () => this.startOnlinePractice() },
      { key: 'school', label: '添加至校本', icon: 'el-icon-collection', handler: () => this.addToSchoolBase() }
    ]
    this.sideActions = actions.filter(a => !(this.portalMode && a.key === 'practice'))
    if (this.$route.query.draft === '1') {
      const draft = loadPaperDraft()
      if (draft) {
        if (draft.items && draft.items.length) {
          this.$store.commit('questionBasket/SET_ITEMS', draft.items)
        }
        this.applyDraftSnapshot(draft)
        if (draft.paperId) this.savedPaperId = draft.paperId
      }
    }
    if (this.$route.query.paperId) {
      this.savedPaperId = Number(this.$route.query.paperId) || this.savedPaperId
    }
    const shareId = this.$route.query.share
    if (shareId) {
      this.loadSharedPaper(String(shareId))
      return
    }
    if (this.$route.query.local === '1') {
      try {
        const raw = sessionStorage.getItem('edu_qb_practice_snapshot')
        const snap = raw ? JSON.parse(raw) : null
        if (snap && snap.items && snap.items.length) {
          this.$store.commit('questionBasket/SET_ITEMS', snap.items)
          this.applyDraftSnapshot(snap)
          return
        }
      } catch (e) { /* ignore */ }
    }
    if (!this.questionBasketItems.length) {
      this.$modal.msgWarning('试题栏为空，请先加入试题')
      if (this.isPortal) {
        this.$router.replace('/chapter')
      }
      return
    }
    if (!this.form.paperTitle) {
      const qTitle = (this.$route.query.title || '').trim()
      this.form.paperTitle = qTitle || this.buildDefaultPaperTitle()
    }
    this.syncSortMode()
    this.syncPageLayoutFromTemplate()
    this.syncScoreTypeDraft()
    this.ensureExportDetails()
    if (this.$route.query.quick === '1' && this.canPreview) {
      this.$nextTick(() => this.exportPaper())
    }
  },
  methods: {
    loadSharedPaper(shareId) {
      getPaperShare(shareId).then(res => {
        const snap = res.data
        if (snap && snap.items && snap.items.length) {
          this.$store.commit('questionBasket/SET_ITEMS', snap.items)
          this.applyDraftSnapshot(snap)
          this.$modal.msgSuccess('已加载分享的试卷')
        } else {
          this.$modal.msgWarning('分享链接无效或已过期')
          this.redirectIfBasketEmpty()
        }
      }).catch(() => {
        if (isServerShareId(shareId)) {
          this.$modal.msgWarning('分享链接无效或已过期，请让分享者重新生成')
          this.redirectIfBasketEmpty()
          return
        }
        const snap = loadPaperShare(shareId)
        if (snap && snap.items && snap.items.length) {
          this.$store.commit('questionBasket/SET_ITEMS', snap.items)
          this.applyDraftSnapshot(snap)
          this.$modal.msgSuccess('已加载分享的试卷（本机缓存）')
        } else {
          this.$modal.msgWarning('分享链接无效或已过期')
          this.redirectIfBasketEmpty()
        }
      })
    },
    redirectIfBasketEmpty() {
      if (!this.questionBasketItems.length) {
        if (this.isPortal) {
          this.$router.replace('/chapter')
        } else {
          this.$router.replace('/admin/question-bank')
        }
      }
    },
    clearBasketAfterSave() {
      this._suppressBasketEmptyWarn = true
      this.$store.commit('questionBasket/CLEAR')
    },
    onSaveDialogClose() {
      this.saveDialogOpen = false
    },
    myPapersRoute() {
      return { path: '/my-papers' }
    },
    handleComposeLeave(next) {
      const finish = () => this.completeLeaveCompose(next)
      const abort = () => {
        if (next) next(false)
      }
      if (!this.questionBasketItems.length) {
        finish()
        return
      }
      this.$msgbox({
        title: '离开组卷',
        message: '是否保存当前试卷？无论是否保存，离开后将清空试题篮。',
        showCancelButton: true,
        confirmButtonText: '保存',
        cancelButtonText: '不保存',
        distinguishCancelAndClose: true,
        type: 'warning'
      }).then(() => {
        this.saveComposePaperSilent().finally(() => finish())
      }).catch(action => {
        if (action === 'cancel') {
          finish()
        } else {
          abort()
        }
      })
    },
    saveComposePaperSilent() {
      const savedAt = Date.now()
      this.saveDraft(false, savedAt)
      if (!this.$store.getters.token) {
        return Promise.resolve()
      }
      if (!this.canPreview) {
        this.$modal.msgWarning('当前试卷未设置分值，已仅保存到本地草稿')
        return Promise.resolve()
      }
      return saveMyPaper(this.buildSavePaperPayload()).then(res => {
        if (res.data) this.savedPaperId = res.data
      }).catch(() => {
        this.$modal.msgError('云端保存失败，已保存到本地草稿')
      })
    },
    completeLeaveCompose(next) {
      this._leavingCompose = true
      this.clearBasketAfterSave()
      const target = this.myPapersRoute()
      if (next) {
        next(target)
      } else {
        this.$router.push(target)
      }
    },
    goBack() {
      this.handleComposeLeave(null)
    },
    showHelp() {
      this.$modal.msgInfo('在左侧预览区点击题目可编辑分值、调整顺序或删除；右侧可设置排序并下载试卷。')
    },
    startOnlinePractice() {
      if (!this.canPreview) {
        this.$modal.msgWarning('请先加入试题并设置分值')
        return
      }
      const snapshot = { ...this.buildShareSnapshot(), practiceMode: true, practiceSource: 'preview' }
      const go = (shareId) => {
        const path = '/admin/question-bank-center/question-bank/paper/practice'
        const query = shareId ? { share: shareId } : { local: '1' }
        if (!shareId) {
          try {
            sessionStorage.setItem('edu_qb_practice_snapshot', JSON.stringify(snapshot))
          } catch (e) { /* ignore */ }
        }
        this.$router.push({ path, query })
      }
      if (!this.$store.getters.token) {
        this.$modal.msgWarning('未登录，仅可在本机浏览器练习')
        go(null)
        return
      }
      createServerPaperShare(snapshot).then(shareId => {
        go(shareId)
      }).catch(() => {
        this.$modal.msgWarning('云端练习快照创建失败，已切换为本机练习')
        go(null)
      })
    },
    addToSchoolBase() {
      if (!this.canPreview) {
        this.$modal.msgWarning('请先加入试题并设置分值')
        return
      }
      this.$confirm('将保存至「我的试卷」，并发布到门户「试卷选题 → 校内试卷」供教师使用。是否继续？', '添加至校本', {
        confirmButtonText: '发布',
        cancelButtonText: '取消',
        type: 'info'
      }).then(() => {
        const payload = this.buildSavePaperPayload()
        const exportConfig = { ...(payload.exportConfig || {}), schoolBase: true }
        payload.exportConfig = exportConfig
        const title = (payload.paperTitle || '').trim()
        if (title && !title.startsWith('[校本]')) {
          payload.paperTitle = `[校本] ${title}`
          this.form.paperTitle = payload.paperTitle
        }
        this.saveDraft(false)
        if (!this.$store.getters.token) {
          this.$modal.msgSuccess('已保存到本地草稿，登录后将同步至云端校本库')
          return
        }
        const schoolPayload = {
          paperTitle: payload.paperTitle,
          items: payload.items
        }
        Promise.all([
          saveMyPaper(payload),
          publishSchoolPaper(schoolPayload)
        ]).then(([saveRes]) => {
          if (saveRes.data) this.savedPaperId = saveRes.data
          this.$modal.msgSuccess('已发布至校本库，可在「我的试卷」或门户「试卷选题-校内试卷」查看')
        }).catch(() => {
          this.$modal.msgError('发布失败，请稍后重试')
        })
      }).catch(() => {})
    },
    applyDraftSnapshot(draft) {
      if (!draft) return
      if (draft.form) {
        this.form = { ...this.form, ...draft.form, header: { ...this.form.header, ...(draft.form.header || {}) } }
      }
      if (draft.groupTab) this.groupTab = draft.groupTab
      if (draft.orderRadio) this.orderRadio = draft.orderRadio
      if (draft.paperTemplate) this.paperTemplate = draft.paperTemplate
      if (draft.answerAreas) this.answerAreas = { ...draft.answerAreas }
      if (draft.exportFormat) this.exportFormat = draft.exportFormat
      if (draft.pageLayout) this.pageLayout = draft.pageLayout
      if (draft.answerSheetOptions) {
        this.answerSheetOptions = { ...this.answerSheetOptions, ...draft.answerSheetOptions }
      }
    },
    openAnalysis() {
      if (!this.canPreview) {
        this.$modal.msgWarning('\u8bf7\u5148\u52a0\u5165\u8bd5\u9898\u5e76\u8bbe\u7f6e\u5206\u503c')
        return
      }
      this.analysisOpen = true
    },
    confirmSaveDraft() {
      if (!this.canPreview) {
        this.$modal.msgWarning('\u8bf7\u5148\u52a0\u5165\u8bd5\u9898\u5e76\u8bbe\u7f6e\u5206\u503c')
        return
      }
      const savedAt = Date.now()
      this.saveDraft(false, savedAt)
      const finish = () => {
        this.saveDialogInfo = {
          title: this.form.paperTitle || '\u672a\u547d\u540d\u8bd5\u5377',
          count: this.basketCount,
          score: this.basketTotalScore,
          savedAtText: new Date(savedAt).toLocaleString()
        }
        this.saveDialogOpen = true
      }
      if (this.$store.getters.token) {
        saveMyPaper(this.buildSavePaperPayload()).then(res => {
          if (res.data) this.savedPaperId = res.data
          finish()
        }).catch(() => {
          this.$modal.msgError('云端保存失败，已保存到本地草稿')
          finish()
        })
        return
      }
      finish()
    },
    buildSavePaperPayload() {
      const paperId = this.savedPaperId || (this.$route.query.paperId ? Number(this.$route.query.paperId) : null)
      return {
        paperId: paperId || undefined,
        paperTitle: this.form.paperTitle,
        templateCode: this.form.templateCode,
        sortMode: this.form.sortMode,
        exportMode: this.form.exportMode,
        answerLayout: this.form.answerLayout,
        header: this.form.header,
        exportConfig: this.form.exportConfig,
        groupTab: this.groupTab,
        orderRadio: this.orderRadio,
        paperTemplate: this.paperTemplate,
        exportFormat: this.exportFormat,
        pageLayout: this.pageLayout,
        answerAreas: this.answerAreas,
        basketItems: this.questionBasketItems.map(item => ({ ...item })),
        items: this.questionBasketItems.map((item, idx) => ({
          questionId: item.questionId,
          orderNum: item.orderNum || idx + 1,
          scoreValue: item.scoreValue
        }))
      }
    },
    openAnswerSheetWorkshop() {
      if (!this.canPreview) {
        this.$modal.msgWarning('\u8bf7\u5148\u52a0\u5165\u8bd5\u9898\u5e76\u8bbe\u7f6e\u5206\u503c')
        return
      }
      this.answerSheetWorkshopMode = true
      this.answerSheetOpen = true
    },
    downloadAnswerSheetQuick() {
      if (!this.canPreview) {
        this.$modal.msgWarning('\u8bf7\u5148\u52a0\u5165\u8bd5\u9898\u5e76\u8bbe\u7f6e\u5206\u503c')
        return
      }
      this.sheetLoading = true
      this.ensureExportPaid().then(() => exportAnswerSheetClient(this, { ...this.answerSheetOptions })).then(() => {
        this.$modal.msgSuccess('\u7b54\u9898\u5361 PDF \u5bfc\u51fa\u6210\u529f')
        this.saveDraft(false)
      }).catch(err => {
        const msg = (err && err.message) || ''
        if (msg === 'cancelled' || msg === 'login') return
        console.error('answer sheet export failed', err)
        this.$modal.msgError('\u7b54\u9898\u5361 PDF \u5bfc\u51fa\u5931\u8d25\uFF0C\u8bf7\u7a0d\u540e\u91cd\u8bd5')
      }).finally(() => { this.sheetLoading = false })
    },
    onAnswerSheetDialogOpen() {
      this.refreshAnswerSheetPreview()
    },
    refreshAnswerSheetPreview() {
      if (!this.canPreview) return
      this.answerSheetPreviewLoading = true
      buildAnswerSheetPreviewHtml(this, { ...this.answerSheetOptions }).then(html => {
        this.answerSheetPreviewHtml = html
      }).catch(err => {
        console.error('answer sheet preview failed', err)
        this.answerSheetPreviewHtml = ''
      }).finally(() => { this.answerSheetPreviewLoading = false })
    },
    printAnswerSheet() {
      if (!this.canPreview) return
      this.sheetLoading = true
      printAnswerSheetClient(this, { ...this.answerSheetOptions }).then(() => {
        this.$modal.msgSuccess('\u5df2\u6253\u5f00\u6253\u5370\u7a97\u53e3')
      }).catch(err => {
        console.error('answer sheet print failed', err)
        this.$modal.msgError('\u6253\u5370\u5931\u8d25\uFF0C\u8bf7\u5141\u8bb8\u5f39\u7a97\u6216\u7a0d\u540e\u91cd\u8bd5')
      }).finally(() => { this.sheetLoading = false })
    },
    confirmAnswerSheetDocxExport() {
      if (!this.canPreview) return
      this.sheetLoading = true
      this.ensureExportPaid().then(() => exportAnswerSheetDocxClient(this, { ...this.answerSheetOptions })).then(() => {
        this.$modal.msgSuccess('答题卡 Word 导出成功')
        this.answerSheetOpen = false
        this.saveDraft(false)
      }).catch(err => {
        const msg = (err && err.message) || ''
        if (msg === 'cancelled' || msg === 'login') return
        console.error('answer sheet docx export failed', err)
        this.$modal.msgError('答题卡 Word 导出失败，请稍后重试')
      }).finally(() => { this.sheetLoading = false })
    },
    confirmAnswerSheetExport() {
      if (!this.canPreview) return
      this.sheetLoading = true
      this.ensureExportPaid().then(() => exportAnswerSheetClient(this, { ...this.answerSheetOptions })).then(() => {
        this.$modal.msgSuccess('\u7b54\u9898\u5361\u5bfc\u51fa\u6210\u529f')
        this.answerSheetOpen = false
        this.saveDraft(false)
      }).catch(err => {
        const msg = (err && err.message) || ''
        if (msg === 'cancelled' || msg === 'login') return
        console.error('answer sheet export failed', err)
        this.$modal.msgError('\u7b54\u9898\u5361\u5bfc\u51fa\u5931\u8d25\uFF0C\u8bf7\u7a0d\u540e\u91cd\u8bd5')
      }).finally(() => { this.sheetLoading = false })
    },
    sharePaper() {
      if (!this.canPreview) {
        this.$modal.msgWarning('请先加入试题并设置分值')
        return
      }
      const snapshot = this.buildShareSnapshot()
      const openShare = (id, mode) => {
        this.shareLinkMode = mode
        this.shareLink = buildPaperShareUrl(this.sharePreviewPath, id)
        this.shareDialogOpen = true
      }
      if (!this.$store.getters.token) {
        this.$confirm(
          '跨设备分享需先登录。是否仅生成本机临时链接？',
          '分享试卷',
          { confirmButtonText: '本机链接', cancelButtonText: '去登录', type: 'info' }
        ).then(() => {
          openShare(createLocalPaperShare(snapshot), 'local')
        }).catch(action => {
          if (action === 'cancel' && this.isPortal) {
            goPortalLogin(this.$router, this.$route.fullPath)
          }
        })
        return
      }
      createServerPaperShare(snapshot).then(id => {
        openShare(id, 'server')
      }).catch(() => {
        this.$modal.confirm(
          '服务端分享失败。可生成本机临时链接（仅当前浏览器可用），或稍后重试。',
          '分享失败',
          { confirmButtonText: '本机链接', cancelButtonText: '取消', type: 'warning' }
        ).then(() => {
          openShare(createLocalPaperShare(snapshot), 'local')
        }).catch(() => {})
      })
    },
    buildDefaultPaperTitle() {
      const d = new Date()
      const y = d.getFullYear()
      const m = String(d.getMonth() + 1).padStart(2, '0')
      const day = String(d.getDate()).padStart(2, '0')
      const subject = this.form.header.subjectName ? this.form.header.subjectName : ''
      const suffix = TEMPLATE_SUFFIX[this.paperTemplate] || '测试卷'
      return `${y}年${m}月${day}日${subject}${suffix}`
    },
    buildShareSnapshot() {
      return {
        form: this.form,
        groupTab: this.groupTab,
        orderRadio: this.orderRadio,
        paperTemplate: this.paperTemplate,
        exportFormat: this.exportFormat,
        pageLayout: this.pageLayout,
        answerAreas: this.answerAreas,
        answerSheetOptions: { ...this.answerSheetOptions },
        items: this.questionBasketItems.map(item => ({ ...item })),
        savedAt: Date.now()
      }
    },
    copyShareLink() {
      const text = this.shareLink
      if (!text) return
      if (navigator.clipboard && navigator.clipboard.writeText) {
        navigator.clipboard.writeText(text).then(() => {
          this.$modal.msgSuccess('\u94fe\u63a5\u5df2\u590d\u5236')
        }).catch(() => this.fallbackCopyShareLink(text))
        return
      }
      this.fallbackCopyShareLink(text)
    },
    fallbackCopyShareLink(text) {
      const ta = document.createElement('textarea')
      ta.value = text
      ta.style.position = 'fixed'
      ta.style.left = '-9999px'
      document.body.appendChild(ta)
      ta.select()
      try {
        document.execCommand('copy')
        this.$modal.msgSuccess('\u94fe\u63a5\u5df2\u590d\u5236')
      } catch (e) {
        this.$modal.msgWarning('\u590d\u5236\u5931\u8d25\uFF0C\u8bf7\u624b\u52a8\u590d\u5236\u94fe\u63a5')
      }
      document.body.removeChild(ta)
    },
    typeLabel(type) {
      return getQuestionTypeLabel(type)
    },
    setGroupTab(key) {
      this.groupTab = key
      this.syncSortMode()
    },
    syncSortMode() {
      if (this.groupTab === 'order') {
        this.form.sortMode = 'BASKET_ORDER'
        return
      }
      if (this.groupTab === 'knowledge') {
        this.form.sortMode = 'BASKET_ORDER'
        return
      }
      this.form.sortMode = this.orderRadio === 'difficulty' ? 'TYPE_THEN_DIFFICULTY' : 'BASKET_ORDER'
    },
    onTemplateChange() {
      if (!this.form.paperTitle || this.form.paperTitle.includes('日')) {
        this.form.paperTitle = this.buildDefaultPaperTitle()
      }
    },
    onTitleBlur() {
      this.saveDraft(false)
    },
    onExportModeChange(val) {
      if (val === 'teacher') {
        this.ensureTeacherDetails()
      }
    },
    onQuestionLeave() {
      // keep toolbar visible briefly; no-op to avoid flicker
    },
    onQuestionClick(q, event) {
      if (event.target.closest('.question-toolbar') ||
          event.target.closest('.question-image') ||
          event.target.closest('.el-image__preview') ||
          event.target.closest('.el-input-number')) {
        return
      }
      this.activeQuestionId = q.questionId
      const id = q.questionId
      if (this.answerExpanded[id]) {
        this.$delete(this.answerExpanded, id)
        return
      }
      this.$set(this.answerExpanded, id, true)
      this.ensureQuestionDetail(id)
    },
    ensureQuestionDetail(questionId) {
      if (this.detailMap[questionId]) return Promise.resolve()
      this.detailLoading = true
      return getQuestion(questionId).then(res => {
        if (res.data) {
          this.$set(this.detailMap, questionId, res.data)
        }
      }).catch(() => {}).finally(() => { this.detailLoading = false })
    },
    sortBasketItems(items) {
      const list = items.map(i => ({ ...i }))
      if (this.groupTab === 'order') {
        return list.sort((a, b) => a.orderNum - b.orderNum)
      }
      if (this.groupTab === 'knowledge') {
        return list.sort((a, b) => a.orderNum - b.orderNum)
      }
      const map = {}
      list.forEach(item => {
        const type = item.questionType || 'short'
        if (!map[type]) map[type] = []
        map[type].push(item)
      })
      const typeOrder = this.questionTypeOrder.length ? this.questionTypeOrder : getQuestionTypeOrder()
      const result = []
      typeOrder.forEach(type => {
        if (!map[type]) return
        const group = [...map[type]]
        if (this.orderRadio === 'difficulty') {
          group.sort((a, b) => (Number(a.difficulty) || 0) - (Number(b.difficulty) || 0))
        } else {
          group.sort((a, b) => a.orderNum - b.orderNum)
        }
        result.push(...group)
      })
      list.forEach(item => {
        const type = item.questionType || 'short'
        if (!typeOrder.includes(type) && !result.includes(item)) {
          result.push(item)
        }
      })
      return result
    },
    countSections(list) {
      const types = new Set(list.map(i => i.questionType || 'short'))
      const typeOrder = this.questionTypeOrder.length ? this.questionTypeOrder : getQuestionTypeOrder()
      return typeOrder.filter(t => types.has(t)).length
    },
    displayContent(item) {
      const detail = this.detailMap[item.questionId]
      const raw = (detail && detail.content) || item.content || item.contentBrief || ''
      return stripLeadingQuestionNo(raw)
    },
    parseJsonArray(raw) {
      if (!raw) return []
      if (Array.isArray(raw)) return raw
      try {
        const arr = JSON.parse(raw)
        return Array.isArray(arr) ? arr : []
      } catch (e) {
        return []
      }
    },
    imageUrls(item) {
      const detail = this.detailMap[item.questionId]
      const raw = (detail && detail.images != null) ? detail.images : item.images
      return this.parseJsonArray(raw)
    },
    previewSrcList(item) {
      return this.imageUrls(item).map(u => this.resolveImageUrl(u))
    },
    optionItems(item) {
      const detail = this.detailMap[item.questionId]
      const raw = (detail && detail.options != null) ? detail.options : item.options
      const arr = this.parseJsonArray(raw)
      if (!arr.length) return []
      if (!isPaperChoiceVolumeType(item.questionType)) return []
      return arr.map((text, i) => parseQuestionOption(text, i))
    },
    resolveImageUrl(url) {
      if (!url) return ''
      if (/^https?:\/\//i.test(url)) return url
      return process.env.VUE_APP_BASE_API + url
    },
    teacherDetail(item) {
      return this.detailMap[item.questionId] || {}
    },
    teacherKnowledge(item) {
      const d = this.teacherDetail(item)
      if (d.chapterText) return d.chapterText
      if (d.knowledgeTags) {
        try {
          const tags = typeof d.knowledgeTags === 'string' ? JSON.parse(d.knowledgeTags) : d.knowledgeTags
          if (Array.isArray(tags) && tags.length) return tags.join('、')
        } catch (e) { /* ignore */ }
      }
      return ''
    },
    teacherAnswer(item) {
      const d = this.teacherDetail(item)
      const raw = d.correctAnswer
      if (raw == null || raw === '') return ''
      return formatChoiceAnswer(item.questionType, raw)
    },
    teacherAnalysis(item) {
      const d = this.teacherDetail(item)
      return d.analysis || ''
    },
    ensureTeacherDetails() {
      const missing = this.questionBasketItems
        .map(i => i.questionId)
        .filter(id => !this.detailMap[id])
      if (!missing.length) return Promise.resolve()
      this.detailLoading = true
      return Promise.all(missing.map(id => getQuestion(id).then(res => {
        if (res.data) {
          this.$set(this.detailMap, id, res.data)
        }
      }).catch(() => {}))).finally(() => { this.detailLoading = false })
    },
    ensureExportDetails() {
      const ids = this.questionBasketItems.map(i => i.questionId)
      if (!ids.length) return Promise.resolve()
      this.detailLoading = true
      return Promise.all(ids.map(id => getQuestion(id).then(res => {
        if (res.data) {
          this.$set(this.detailMap, id, res.data)
        }
      }).catch(() => {}))).finally(() => { this.detailLoading = false })
    },
    setScore(questionId, scoreValue) {
      this.$store.commit('questionBasket/SET_SCORE', { questionId, scoreValue })
      this.saveDraft(false)
    },
    distributeScoreEvenly() {
      if (!this.questionBasketItems.length) return
      this.$store.commit('questionBasket/DISTRIBUTE_SCORE', this.targetTotalScore)
      this.syncScoreTypeDraft()
      this.saveDraft(false)
      this.$modal.msgSuccess(`已按目标总分 ${this.targetTotalScore} 分平均分配`)
    },
    applyTypeScore(questionType, scoreValue) {
      const score = Number(scoreValue) || 0
      this.$set(this.scoreTypeDraft, questionType, score)
      this.$store.commit('questionBasket/SET_SCORE_BY_TYPE', { questionType, scoreValue: score })
      this.saveDraft(false)
    },
    syncScoreTypeDraft() {
      const draft = {}
      groupItemsByQuestionType(this.questionBasketItems).forEach(g => {
        const first = g.items[0]
        draft[g.type] = first && first.scoreValue != null ? Number(first.scoreValue) : 5
      })
      this.scoreTypeDraft = draft
    },
    removeQuestion(questionId) {
      this.$modal.confirm('确认从本卷移除该题？').then(() => {
        this.$store.commit('questionBasket/REMOVE', questionId)
        this.$delete(this.answerAreas, questionId)
        this.$delete(this.answerExpanded, questionId)
        if (this.activeQuestionId === questionId) {
          this.activeQuestionId = null
        }
        this.saveDraft(false)
      }).catch(() => {})
    },
    moveQuestion(questionId, direction) {
      const flat = this.sortedItems.map(i => i.questionId)
      const idx = flat.indexOf(questionId)
      if (idx < 0) return
      const target = idx + direction
      if (target < 0 || target >= flat.length) return
      const next = [...flat]
      const tmp = next[idx]
      next[idx] = next[target]
      next[target] = tmp
      const map = {}
      this.questionBasketItems.forEach(i => { map[i.questionId] = i })
      const reordered = next.map(id => map[id]).filter(Boolean)
      this.$store.commit('questionBasket/REORDER', reordered)
      this.groupTab = 'order'
      this.orderRadio = 'order'
      this.form.sortMode = 'BASKET_ORDER'
    },
    scrollToQuestion(questionId) {
      this.activeQuestionId = questionId
      this.$nextTick(() => {
        const ref = this.$refs['q-' + questionId]
        const el = Array.isArray(ref) ? ref[0] : ref
        if (el && el.scrollIntoView) {
          el.scrollIntoView({ behavior: 'smooth', block: 'center' })
        }
      })
    },
    openDetail(item) {
      this.detailQuestionId = item.questionId
      this.detailOpen = true
    },
    openReplace(item) {
      this.replaceInitialTab = 'search'
      this.replaceQuestion = item
      this.replaceOpen = true
    },
    openSimilarReplace(item) {
      this.replaceInitialTab = 'similar'
      this.replaceQuestion = item
      this.replaceOpen = true
    },
    onReplaceQuestion({ oldQuestionId, newQuestion }) {
      const area = this.answerAreas[oldQuestionId]
      this.$store.commit('questionBasket/REPLACE', { oldQuestionId, newQuestion })
      if (area) {
        this.$set(this.answerAreas, newQuestion.questionId, area)
        this.$delete(this.answerAreas, oldQuestionId)
      }
      if (this.detailMap[oldQuestionId]) {
        this.$set(this.detailMap, newQuestion.questionId, newQuestion)
        this.$delete(this.detailMap, oldQuestionId)
      }
      this.activeQuestionId = newQuestion.questionId
      this.saveDraft(false)
    },
    openFeedback(item) {
      this.feedbackQuestion = item
      this.feedbackOpen = true
    },
    openAnswerArea(item) {
      this.answerAreaQuestion = item
      this.answerAreaOpen = true
    },
    getAnswerArea(questionId) {
      return this.answerAreas[questionId] || null
    },
    blankAreaStyle(questionId) {
      const area = this.getAnswerArea(questionId)
      if (!area) return {}
      return { minHeight: `${Math.max(48, area.lines * 28)}px` }
    },
    onAnswerAreaConfirm(payload) {
      if (!this.answerAreaQuestion) return
      this.$set(this.answerAreas, this.answerAreaQuestion.questionId, {
        style: payload.style,
        lines: payload.lines
      })
      this.saveDraft(false)
      this.$modal.msgSuccess('作答区已插入')
    },
    onAnswerAreaClear() {
      if (!this.answerAreaQuestion) return
      this.$delete(this.answerAreas, this.answerAreaQuestion.questionId)
      this.saveDraft(false)
      this.$modal.msgSuccess('作答区已移除')
    },
    buildPayload() {
      return {
        paperTitle: this.form.paperTitle,
        header: { ...this.form.header, totalScore: this.basketTotalScore },
        templateCode: this.form.templateCode,
        sortMode: this.form.sortMode,
        exportMode: this.form.exportMode,
        exportConfig: this.form.exportConfig,
        items: this.questionBasketItems.map(item => {
          const area = this.answerAreas[item.questionId]
          const payload = {
            questionId: item.questionId,
            orderNum: item.orderNum,
            scoreValue: item.scoreValue
          }
          if (area && area.lines > 0) {
            payload.answerAreaLines = area.lines
            payload.answerAreaStyle = area.style || 'ruled'
          }
          return payload
        })
      }
    },
    saveDraft(showMsg = true, savedAt) {
      const ts = savedAt || Date.now()
      savePaperDraft({
        form: this.form,
        groupTab: this.groupTab,
        orderRadio: this.orderRadio,
        paperTemplate: this.paperTemplate,
        exportFormat: this.exportFormat,
        pageLayout: this.pageLayout,
        answerAreas: this.answerAreas,
        items: this.questionBasketItems.map(item => ({ ...item })),
        paperId: this.savedPaperId,
        answerSheetOptions: { ...this.answerSheetOptions },
        savedAt: ts,
        itemCount: this.questionBasketItems.length
      })
      if (showMsg) {
        this.$modal.msgSuccess('试卷已保存草稿')
      }
    },
    syncPageLayoutFromTemplate() {
      const code = (this.form.templateCode || 'A4_1COL').toUpperCase()
      this.pageLayout = code.startsWith('A3') ? 'A3' : 'A4'
    },
    onPageLayoutChange() {
      this.form.templateCode = this.pageLayout === 'A3' ? 'A3_1COL' : 'A4_1COL'
    },
    exportPaper() {
      if (!this.canPreview) {
        this.$modal.msgWarning('请确保试题栏有题目且总分大于 0')
        return
      }
      this.onPageLayoutChange()
      this.ensureExportPaid().then(() => {
        this.exportLoading = true
        return exportPaperClient(this)
      }).then(() => {
        const label = this.exportFormat === 'docx' ? 'Word' : 'PDF'
        this.$modal.msgSuccess(`${label} 导出成功`)
        this.saveDraft(false)
      }).catch(err => {
        const msg = (err && err.message) || ''
        if (msg === 'cancelled' || msg === 'login') return
        console.error('paper export failed', err)
        this.$modal.msgError('导出失败，请稍后重试')
      }).finally(() => { this.exportLoading = false })
    },
    buildExportBizRef() {
      const key = 'edu_paper_export_ref'
      let ref = sessionStorage.getItem(key)
      if (!ref) {
        ref = 'exp' + Date.now() + Math.floor(Math.random() * 10000)
        sessionStorage.setItem(key, ref)
      }
      return ref
    },
    ensureExportPaid() {
      if (!this.token) {
        goPortalLogin(this.$router, this.$route.fullPath)
        return Promise.reject(new Error('login'))
      }
      const bizId = this.savedPaperId || 0
      return checkPayAccess({
        bizType: PAY_BIZ.PAPER_EXPORT,
        bizId,
        bizRef: this.exportBizRef
      }).then(res => {
        const info = res.data || {}
        if (!info.needPay || info.purchased) return
        return this.$refs.payDialog.open({
          bizType: PAY_BIZ.PAPER_EXPORT,
          bizId,
          bizRef: this.exportBizRef,
          title: '组卷导出',
          productLabel: this.form.paperTitle || '试卷导出'
        })
      })
    }
  }
}
</script>

<style scoped lang="scss">
.paper-compose-page {
  background: #eef2f8;
  min-height: calc(100vh - 84px);
  padding-bottom: 24px;
}
.compose-topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}
.compose-breadcrumb {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #8a96a8;
  .crumb-link {
    cursor: pointer;
    &:hover { color: #409eff; }
  }
  .crumb-current { color: #303133; font-weight: 600; }
}
.compose-help {
  font-size: 13px;
  color: #409eff;
  text-decoration: none;
}
.compose-layout { align-items: flex-start; }
.paper-canvas-wrap { min-height: 640px; }
.paper-empty {
  background: #fff;
  border-radius: 4px;
  min-height: 400px;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
}
.paper-canvas {
  background: #fff;
  border-radius: 4px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  padding: 36px 48px 48px;
  min-height: 640px;
}
.paper-title-row {
  text-align: center;
  margin-bottom: 18px;
}
.paper-title-input {
  max-width: 520px;
  ::v-deep .el-input__inner {
    border: none;
    text-align: center;
    font-size: 20px;
    font-weight: 700;
    color: #222;
    background: transparent;
    padding: 0;
    height: 36px;
    line-height: 36px;
    &:focus { border-bottom: 1px solid #409eff; border-radius: 0; }
  }
}
.paper-meta-line {
  display: flex;
  justify-content: center;
  flex-wrap: wrap;
  gap: 28px;
  margin-bottom: 28px;
  font-size: 14px;
  color: #333;
}
.meta-field {
  display: inline-flex;
  align-items: flex-end;
  gap: 4px;
}
.meta-blank {
  display: inline-block;
  width: 72px;
  border-bottom: 1px solid #333;
  height: 1em;
  &.wide { width: 96px; }
}
.volume-title {
  text-align: center;
  font-size: 16px;
  font-weight: 700;
  margin: 20px 0 12px;
}
.section-title {
  font-size: 14px;
  font-weight: 700;
  margin: 16px 0 10px;
}
.paper-question {
  position: relative;
  margin: 12px 0 20px;
  padding: 12px 14px 14px;
  border: 2px dashed transparent;
  border-radius: 4px;
  cursor: pointer;
  transition: border-color 0.15s, box-shadow 0.15s, background 0.15s;
  &:hover,
  &.is-active,
  &.is-answer-expanded {
    border-color: #409eff;
    border-style: dashed;
    background: #f7fbff;
    box-shadow: 0 0 0 1px rgba(64, 158, 255, 0.15);
  }
}
.question-toolbar {
  position: absolute;
  top: -2px;
  right: 8px;
  transform: translateY(-100%);
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 4px;
  background: #409eff;
  color: #fff;
  padding: 4px 8px;
  border-radius: 4px 4px 0 0;
  font-size: 12px;
  z-index: 2;
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.35);
}
.tb-btn {
  border: none;
  background: transparent;
  color: #fff;
  cursor: pointer;
  padding: 2px 6px;
  font-size: 12px;
  &:hover { text-decoration: underline; }
}
.tb-score {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  margin: 0 4px;
  em { font-style: normal; font-size: 11px; opacity: 0.95; }
  ::v-deep .el-input-number {
    width: 88px;
    line-height: 24px;
    .el-input__inner {
      height: 24px;
      line-height: 24px;
      padding-left: 4px;
      padding-right: 28px;
    }
  }
}
.tb-icon {
  border: none;
  background: rgba(255, 255, 255, 0.15);
  color: #fff;
  width: 24px;
  height: 24px;
  border-radius: 3px;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  &:hover { background: rgba(255, 255, 255, 0.28); }
  &.danger:hover { background: #f56c6c; }
}
.question-stem {
  display: flex;
  align-items: flex-start;
  gap: 4px;
  line-height: 1.7;
  font-size: 14px;
  color: #222;
}
.q-no { flex-shrink: 0; font-weight: 600; }
.question-images {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin: 10px 0 10px 20px;
}
.question-image {
  width: 180px;
  height: 120px;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  background: #fafafa;
}
.question-options {
  list-style: none;
  margin: 8px 0 0 20px;
  padding: 0;
  display: flex;
  flex-wrap: wrap;
  gap: 8px 24px;
}
.option-item {
  display: inline-flex;
  align-items: flex-start;
  gap: 4px;
  font-size: 14px;
  min-width: 22%;
}
.option-label { font-weight: 600; }
.teacher-block {
  margin-top: 12px;
  padding-top: 10px;
  border-top: 1px dashed #e4e7ed;
  font-size: 13px;
  color: #606266;
  line-height: 1.7;
}
.answer-expand-block {
  margin-top: 12px;
  padding: 10px 12px;
  background: #f0f7ff;
  border: 1px solid #d9ecff;
  border-radius: 4px;
  font-size: 13px;
  color: #606266;
  line-height: 1.7;
}
.teacher-row { margin: 4px 0; }
.teacher-empty { color: #909399; font-style: italic; }
.teacher-tag { color: #409eff; font-weight: 600; margin-right: 4px; }
.answer-area-block { margin: 12px 0 4px 20px; }
.answer-area-line {
  border-bottom: 1px solid #999;
  height: 28px;
  margin-bottom: 6px;
}
.answer-area-blank {
  border: 1px dashed #bbb;
  border-radius: 2px;
  background: #fafafa;
}
.sidebar-stack {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.sidebar-card {
  background: #fff;
  border-radius: 4px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
  padding: 14px 16px;
}
.stats-card {
  display: flex;
  justify-content: space-between;
  text-align: center;
  .stat-item {
    flex: 1;
    label {
      display: block;
      font-size: 12px;
      color: #909399;
      margin-bottom: 4px;
    }
    b { font-size: 18px; color: #303133; }
  }
}
.action-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px 12px;
  margin-bottom: 14px;
}
.action-link {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #606266;
  text-decoration: none;
  padding: 4px 0;
  i { color: #409eff; font-size: 16px; }
  &:hover { color: #409eff; }
}
.export-options {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px dashed #e8edf3;
}
.export-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}
.export-label {
  font-size: 12px;
  color: #606266;
  flex-shrink: 0;
  margin-right: 8px;
}
.export-radios {
  flex: 1;
  display: flex;
  justify-content: flex-end;
}
.download-btn {
  width: 100%;
  font-size: 15px;
  padding: 12px 0;
  border-radius: 4px;
}
.score-quick-card {
  .score-quick-row {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 12px;
    flex-wrap: wrap;
  }
  .score-quick-label {
    font-size: 12px;
    color: #606266;
    white-space: nowrap;
  }
  .score-type-row {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 8px;
    padding: 6px 0;
    border-top: 1px dashed #eef2f6;
    &:first-of-type { border-top: none; }
  }
  .score-type-label {
    font-size: 12px;
    color: #303133;
    em {
      font-style: normal;
      color: #909399;
      margin-left: 4px;
    }
  }
  .score-type-input {
    display: inline-flex;
    align-items: center;
    gap: 4px;
    flex-shrink: 0;
  }
  .score-type-unit {
    font-size: 11px;
    color: #909399;
  }
}
.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 12px;
}
.template-radios,
.order-radios {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 16px;
}
.group-tabs {
  display: flex;
  border-bottom: 1px solid #ebeef5;
  margin-bottom: 12px;
}
.group-tab {
  flex: 1;
  text-align: center;
  padding: 8px 4px;
  font-size: 13px;
  color: #606266;
  cursor: pointer;
  border-bottom: 2px solid transparent;
  margin-bottom: -1px;
  &.active {
    color: #409eff;
    border-bottom-color: #409eff;
    font-weight: 600;
  }
}
.nav-map { margin-top: 14px; }
.nav-volume-title {
  font-size: 13px;
  font-weight: 700;
  color: #303133;
  margin: 8px 0 6px;
}
.nav-section-title {
  font-size: 12px;
  color: #606266;
  margin-bottom: 6px;
}
.nav-boxes {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 10px;
}
.nav-box {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 28px;
  height: 28px;
  padding: 0 6px;
  border: 1px solid #dcdfe6;
  border-radius: 3px;
  font-size: 12px;
  color: #606266;
  cursor: pointer;
  background: #fff;
  &:hover,
  &.active {
    border-color: #409eff;
    color: #409eff;
    background: #ecf5ff;
  }
}
.detail-body { font-size: 14px; }
.detail-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 12px 24px;
  margin-bottom: 16px;
  font-size: 13px;
  color: #606266;
}
.detail-meta-item label {
  color: #909399;
  margin-right: 6px;
}
.detail-stem { line-height: 1.7; margin-bottom: 12px; }
.detail-images {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 12px;
}
.detail-image {
  width: 240px;
  height: 150px;
  border: 1px solid #ebeef5;
  border-radius: 4px;
}
.detail-footer {
  border-top: 1px solid #ebeef5;
  padding-top: 12px;
  line-height: 1.7;
  color: #606266;
}
.detail-footer-row { margin: 6px 0; }


.analysis-body { font-size: 13px; }
.analysis-summary {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 10px;
  margin-bottom: 16px;
}
.analysis-stat {
  background: #f5f7fa;
  border-radius: 6px;
  padding: 10px;
  text-align: center;
}
.analysis-stat label {
  display: block;
  font-size: 12px;
  color: #909399;
  margin-bottom: 4px;
}
.analysis-stat b { font-size: 18px; color: #303133; }
.analysis-block { margin-bottom: 16px; }
.analysis-block-title {
  font-weight: 600;
  margin-bottom: 8px;
  color: #303133;
}
.diff-bars { display: flex; flex-direction: column; gap: 10px; }
.diff-row { display: flex; align-items: center; gap: 10px; }
.diff-label { width: 40px; font-size: 12px; color: #606266; }
.diff-bar-wrap {
  flex: 1;
  height: 8px;
  background: #ebeef5;
  border-radius: 4px;
  overflow: hidden;
}
.diff-bar { height: 100%; background: #409eff; border-radius: 4px; min-width: 2px; }
.diff-count { width: 28px; text-align: right; font-size: 12px; color: #606266; }
.save-dialog-hint, .share-dialog-hint {
  margin: 0 0 12px;
  font-size: 13px;
  color: #606266;
  line-height: 1.6;
}
.save-dialog-meta {
  background: #f5f7fa;
  border-radius: 6px;
  padding: 12px 14px;
  font-size: 13px;
  color: #303133;
}
.save-dialog-meta div { margin: 6px 0; }
.save-dialog-meta label {
  display: inline-block;
  width: 72px;
  color: #909399;
}


.answer-sheet-workshop-dialog ::v-deep .el-dialog__body { padding-top: 8px; }
.as-workshop { display: flex; gap: 16px; min-height: 420px; }
.as-workshop-left {
  width: 300px; flex-shrink: 0; padding-right: 12px; border-right: 1px solid #ebeef5;
}
.as-workshop-right { flex: 1; min-width: 0; display: flex; flex-direction: column; }
.as-stats-bar {
  display: flex; flex-wrap: wrap; gap: 8px 12px; margin-bottom: 14px; padding: 10px 12px;
  background: #f5f7fa; border-radius: 8px; font-size: 12px; color: #606266;
}
.as-stats-bar span { white-space: nowrap; }
.as-preview-toolbar {
  display: flex; align-items: center; justify-content: space-between;
  margin-bottom: 8px; font-size: 13px; color: #606266; font-weight: 600;
}
.as-preview-scroll {
  flex: 1; overflow: auto; border: 1px solid #dcdfe6; border-radius: 6px;
  background: #eef2f8; padding: 12px; max-height: 520px;
}
.as-preview-paper {
  background: #fff; box-shadow: 0 2px 12px rgba(0,0,0,0.08);
  transform-origin: top center; transform: scale(0.72); width: 794px; margin: 0 auto;
}
.as-workshop-left ::v-deep .el-checkbox { display: block; margin-left: 0; margin-bottom: 4px; }

.paper-compose-page--portal {


  background: #F8FAFC;
  min-height: calc(100vh - 120px);
  padding: 16px 20px 32px;

  .compose-breadcrumb .crumb-link:hover { color: #2563EB; }
  .compose-help { color: #2563EB; }

  .paper-title-input ::v-deep .el-input__inner:focus {
    border-bottom-color: #2563EB;
  }

  .paper-question:hover,
  .paper-question.is-active,
  .paper-question.is-answer-expanded {
    border-color: #2563EB;
    background: #EFF6FF;
    box-shadow: 0 0 0 1px rgba(37, 99, 235, 0.12);
  }

  .question-toolbar {
    background: #2563EB;
    box-shadow: 0 2px 8px rgba(37, 99, 235, 0.28);
  }

  .teacher-tag { color: #2563EB; }

  .answer-expand-block {
    background: #EFF6FF;
    border-color: #BFDBFE;
  }

  .action-link {
    i { color: #2563EB; }
    &:hover { color: #2563EB; }
  }

  .group-tab.active {
    color: #2563EB;
    border-bottom-color: #2563EB;
  }

  .nav-box:hover,
  .nav-box.active {
    border-color: #2563EB;
    color: #2563EB;
    background: #EFF6FF;
  }

  .score-quick-card .el-button--primary.is-plain {
    color: #2563EB;
    border-color: rgba(37, 99, 235, 0.35);
    background: #EFF6FF;
  }

  .sidebar-stack {
    position: sticky;
    top: 108px;
  }

  .download-btn {
    background: linear-gradient(135deg, #7C3AED, #2563EB);
    border-color: transparent;
    border-radius: 10px;
    font-weight: 600;
  }
}
</style>
