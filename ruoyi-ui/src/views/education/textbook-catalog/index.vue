<template>
  <div class="app-container education-page catalog-page">
    <div class="page-header">
      <div>
        <div class="page-title">教材目录管理</div>
        <div class="page-desc">维护学段、学科、版本、教材与章节层级，用于题库选题导航</div>
      </div>
    </div>

    <div class="stage-bar">
      <span class="stage-bar-label">学段</span>
      <el-radio-group v-model="currentSchoolStage" size="small" @change="onStageChange">
        <el-radio-button label="初中">初中</el-radio-button>
        <el-radio-button label="高中">高中</el-radio-button>
      </el-radio-group>
    </div>

    <el-row :gutter="12" class="catalog-row">
      <el-col :xs="24" :sm="12" :md="6">
        <div class="panel-card">
          <div class="panel-head">
            <span>学科</span>
            <el-button v-hasPermi="['education:subject:add']" type="text" icon="el-icon-plus" @click="openSubjectForm()">新增</el-button>
          </div>
          <el-table
            v-loading="subjectLoading"
            :data="subjectList"
            height="520"
            highlight-current-row
            size="mini"
            @current-change="onSubjectSelect"
          >
            <el-table-column prop="subjectName" :label="'名称'" min-width="80" />
            <el-table-column prop="orderNum" :label="'排序'" width="50" align="center" />
            <el-table-column :label="'状态'" width="56" align="center">
              <template slot-scope="scope">
                <el-tag size="mini" :type="scope.row.status === '0' ? 'success' : 'info'">{{ scope.row.status === '0' ? '启用' : '停用' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="" width="72" align="center">
              <template slot-scope="scope">
                <el-button v-hasPermi="['education:subject:edit']" type="text" size="mini" @click.stop="openSubjectForm(scope.row)">修改</el-button>
                <el-button v-hasPermi="['education:subject:remove']" type="text" size="mini" @click.stop="removeSubject(scope.row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-col>

      <el-col :xs="24" :sm="12" :md="6">
        <div class="panel-card">
          <div class="panel-head">
            <span>版本</span>
            <el-button v-hasPermi="['education:textbook:add']" type="text" icon="el-icon-plus" :disabled="!currentSubjectId" @click="openVersionForm()">新增</el-button>
          </div>
          <el-table
            v-loading="versionLoading"
            :data="versionList"
            height="520"
            highlight-current-row
            size="mini"
            @current-change="onVersionSelect"
          >
            <el-table-column prop="versionName" :label="'名称'" min-width="80" />
            <el-table-column prop="schoolStage" :label="'学段'" width="56" align="center" />
            <el-table-column prop="orderNum" :label="'排序'" width="50" align="center" />
            <el-table-column :label="'状态'" width="56" align="center">
              <template slot-scope="scope">
                <el-tag size="mini" :type="scope.row.status === '0' ? 'success' : 'info'">{{ scope.row.status === '0' ? '启用' : '停用' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="" width="72" align="center">
              <template slot-scope="scope">
                <el-button v-hasPermi="['education:textbook:edit']" type="text" size="mini" @click.stop="openVersionForm(scope.row)">修改</el-button>
                <el-button v-hasPermi="['education:textbook:remove']" type="text" size="mini" @click.stop="removeVersion(scope.row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-if="!versionLoading && currentSubjectId && !versionList.length" :image-size="48" :description="'请先选择学科'" />
        </div>
      </el-col>

      <el-col :xs="24" :sm="12" :md="6">
        <div class="panel-card">
          <div class="panel-head">
            <span>教材</span>
            <el-button v-hasPermi="['education:textbook:add']" type="text" icon="el-icon-plus" :disabled="!currentVersionId" @click="openTextbookForm()">新增</el-button>
          </div>
          <el-table
            v-loading="textbookLoading"
            :data="textbookList"
            height="520"
            highlight-current-row
            size="mini"
            @current-change="onTextbookSelect"
          >
            <el-table-column prop="textbookName" :label="'名称'" min-width="90" show-overflow-tooltip />
            <el-table-column prop="orderNum" :label="'排序'" width="50" align="center" />
            <el-table-column :label="'状态'" width="56" align="center">
              <template slot-scope="scope">
                <el-tag size="mini" :type="scope.row.status === '0' ? 'success' : 'info'">{{ scope.row.status === '0' ? '启用' : '停用' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="" width="72" align="center">
              <template slot-scope="scope">
                <el-button v-hasPermi="['education:textbook:edit']" type="text" size="mini" @click.stop="openTextbookForm(scope.row)">修改</el-button>
                <el-button v-hasPermi="['education:textbook:remove']" type="text" size="mini" @click.stop="removeTextbook(scope.row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-col>

      <el-col :xs="24" :sm="12" :md="6">
        <div class="panel-card">
          <div class="panel-head">
            <span>章节</span>
            <div class="panel-actions">
              <el-button v-hasPermi="['education:textbook:add']" type="text" icon="el-icon-plus" :disabled="!currentTextbookId" @click="openChapterForm()">新增章</el-button>
              <el-button v-hasPermi="['education:textbook:add']" type="text" icon="el-icon-plus" :disabled="!currentTextbookId" @click="onAddSectionClick">新增节</el-button>
            </div>
          </div>
          <div v-loading="chapterLoading" class="chapter-tree-wrap">
            <el-tree
              ref="chapterTree"
              :data="chapterTree"
              node-key="id"
              highlight-current
              default-expand-all
              :expand-on-click-node="false"
              @node-click="onChapterNodeClick"
            >
              <span slot-scope="{ node, data }" class="tree-node">
                <span class="tree-label">{{ node.label }}</span>
                <span class="tree-actions">
                  <el-button v-hasPermi="['education:textbook:edit']" type="text" size="mini" @click.stop="openChapterForm(data)">修改</el-button>
                  <el-button v-hasPermi="['education:textbook:remove']" type="text" size="mini" @click.stop="removeChapter(data)">删除</el-button>
                </span>
              </span>
            </el-tree>
            <el-empty v-if="!chapterLoading && currentTextbookId && !chapterTree.length" :image-size="48" :description="'请先选择教材'" />
          </div>
        </div>
      </el-col>
    </el-row>

    <el-dialog :title="subjectDialogTitle" :visible.sync="subjectOpen" width="480px" append-to-body>
      <el-form ref="subjectFormRef" :model="subjectForm" :rules="subjectRules" label-width="88px" size="small">
        <el-form-item :label="'学科名称'" prop="subjectName">
          <el-input v-model="subjectForm.subjectName" />
        </el-form-item>
        <el-form-item :label="'满分'" prop="fullScore">
          <el-input-number v-model="subjectForm.fullScore" :min="0" :precision="2" controls-position="right" style="width:100%" />
        </el-form-item>
        <el-form-item :label="'排序'" prop="orderNum">
          <el-input-number v-model="subjectForm.orderNum" :min="0" controls-position="right" style="width:100%" />
        </el-form-item>
        <el-form-item :label="'状态'" prop="status">
          <el-radio-group v-model="subjectForm.status">
            <el-radio label="0">启用</el-radio>
            <el-radio label="1">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item :label="'备注'" prop="remark">
          <el-input v-model="subjectForm.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="subjectOpen = false">取 消</el-button>
        <el-button type="primary" @click="submitSubject">确 定</el-button>
      </div>
    </el-dialog>

    <el-dialog :title="versionDialogTitle" :visible.sync="versionOpen" width="480px" append-to-body>
      <el-form ref="versionFormRef" :model="versionForm" :rules="versionRules" label-width="88px" size="small">
        <el-form-item :label="'学段'" prop="schoolStage">
          <el-radio-group v-model="versionForm.schoolStage">
            <el-radio label="初中">初中</el-radio>
            <el-radio label="高中">高中</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item :label="'版本名称'" prop="versionName">
          <el-input v-model="versionForm.versionName" />
        </el-form-item>
        <el-form-item :label="'排序'" prop="orderNum">
          <el-input-number v-model="versionForm.orderNum" :min="0" controls-position="right" style="width:100%" />
        </el-form-item>
        <el-form-item :label="'状态'" prop="status">
          <el-radio-group v-model="versionForm.status">
            <el-radio label="0">启用</el-radio>
            <el-radio label="1">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="versionOpen = false">取 消</el-button>
        <el-button type="primary" @click="submitVersion">确 定</el-button>
      </div>
    </el-dialog>

    <el-dialog :title="textbookDialogTitle" :visible.sync="textbookOpen" width="480px" append-to-body>
      <el-form ref="textbookFormRef" :model="textbookForm" :rules="textbookRules" label-width="88px" size="small">
        <el-form-item :label="'教材名称'" prop="textbookName">
          <el-input v-model="textbookForm.textbookName" />
        </el-form-item>
        <el-form-item :label="'排序'" prop="orderNum">
          <el-input-number v-model="textbookForm.orderNum" :min="0" controls-position="right" style="width:100%" />
        </el-form-item>
        <el-form-item :label="'状态'" prop="status">
          <el-radio-group v-model="textbookForm.status">
            <el-radio label="0">启用</el-radio>
            <el-radio label="1">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="textbookOpen = false">取 消</el-button>
        <el-button type="primary" @click="submitTextbook">确 定</el-button>
      </div>
    </el-dialog>

    <el-dialog :title="chapterDialogTitle" :visible.sync="chapterOpen" width="480px" append-to-body>
      <el-form ref="chapterFormRef" :model="chapterForm" :rules="chapterRules" label-width="88px" size="small">
        <el-form-item v-if="chapterForm.parentId" :label="'所属章'">
          <el-input :value="parentChapterLabel" disabled />
        </el-form-item>
        <el-form-item :label="'章节名称'" prop="chapterName">
          <el-input v-model="chapterForm.chapterName" />
        </el-form-item>
        <el-form-item :label="'排序'" prop="orderNum">
          <el-input-number v-model="chapterForm.orderNum" :min="0" controls-position="right" style="width:100%" />
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="chapterOpen = false">取 消</el-button>
        <el-button type="primary" @click="submitChapter">确 定</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listSubject, getSubject, addSubject, updateSubject, delSubject } from '@/api/education/subject'
import {
  listTextbookVersionsAdmin, getTextbookVersion, addTextbookVersion, updateTextbookVersion, delTextbookVersion,
  listTextbooksAdmin, getTextbook, addTextbook, updateTextbook, delTextbook,
  listTextbookChapters, getTextbookChapter, addTextbookChapter, updateTextbookChapter, delTextbookChapter
} from '@/api/education/textbook'

export default {
  name: 'TextbookCatalog',
  data() {
    return {
      subjectLoading: false,
      versionLoading: false,
      textbookLoading: false,
      chapterLoading: false,
      subjectList: [],
      versionList: [],
      textbookList: [],
      chapterFlatList: [],
      currentSchoolStage: '高中',
      currentSubjectId: undefined,
      currentVersionId: undefined,
      currentTextbookId: undefined,
      currentChapterNode: null,
      parentChapterLabel: '',
      subjectOpen: false,
      versionOpen: false,
      textbookOpen: false,
      chapterOpen: false,
      subjectDialogTitle: '',
      versionDialogTitle: '',
      textbookDialogTitle: '',
      chapterDialogTitle: '',
      subjectForm: {},
      versionForm: {},
      textbookForm: {},
      chapterForm: {},
      subjectRules: {
        subjectName: [{ required: true, message: '学科名称', trigger: 'blur' }]
      },
      versionRules: {
        schoolStage: [{ required: true, message: '学段', trigger: 'change' }],
        versionName: [{ required: true, message: '版本名称', trigger: 'blur' }]
      },
      textbookRules: {
        textbookName: [{ required: true, message: '教材名称', trigger: 'blur' }]
      },
      chapterRules: {
        chapterName: [{ required: true, message: '章节名称', trigger: 'blur' }]
      }
    }
  },
  computed: {
    chapterTree() {
      const roots = []
      const map = {}
      this.chapterFlatList.forEach(item => {
        map[item.chapterId] = { id: String(item.chapterId), label: item.chapterName, raw: item, children: [] }
      })
      this.chapterFlatList.forEach(item => {
        const node = map[item.chapterId]
        if (item.parentId && map[item.parentId]) {
          map[item.parentId].children.push(node)
        } else {
          roots.push(node)
        }
      })
      return roots
    }
  },
  created() {
    this.loadSubjects()
  },
  methods: {
    loadSubjects() {
      this.subjectLoading = true
      listSubject({ pageNum: 1, pageSize: 500 }).then(res => {
        this.subjectList = (res.rows || []).filter(s => s.subjectName !== '总分')
      }).finally(() => { this.subjectLoading = false })
    },
    onStageChange() {
      this.currentVersionId = undefined
      this.currentTextbookId = undefined
      this.versionList = []
      this.textbookList = []
      this.chapterFlatList = []
      if (this.currentSubjectId) {
        this.loadVersions()
      }
    },
    onSubjectSelect(row) {
      if (!row) return
      this.currentSubjectId = row.subjectId
      this.currentVersionId = undefined
      this.currentTextbookId = undefined
      this.versionList = []
      this.textbookList = []
      this.chapterFlatList = []
      this.loadVersions()
    },
    loadVersions() {
      if (!this.currentSubjectId) return
      this.versionLoading = true
      listTextbookVersionsAdmin(this.currentSubjectId, this.currentSchoolStage).then(res => {
        this.versionList = res.data || []
      }).finally(() => { this.versionLoading = false })
    },
    onVersionSelect(row) {
      if (!row) return
      this.currentVersionId = row.versionId
      this.currentTextbookId = undefined
      this.textbookList = []
      this.chapterFlatList = []
      this.loadTextbooks()
    },
    loadTextbooks() {
      if (!this.currentVersionId) return
      this.textbookLoading = true
      listTextbooksAdmin(this.currentVersionId).then(res => {
        this.textbookList = res.data || []
      }).finally(() => { this.textbookLoading = false })
    },
    onTextbookSelect(row) {
      if (!row) return
      this.currentTextbookId = row.textbookId
      this.currentChapterNode = null
      this.loadChapters()
    },
    loadChapters() {
      if (!this.currentTextbookId) return
      this.chapterLoading = true
      listTextbookChapters(this.currentTextbookId).then(res => {
        this.chapterFlatList = res.data || []
      }).finally(() => { this.chapterLoading = false })
    },
    onChapterNodeClick(data) {
      this.currentChapterNode = data
    },
    onAddSectionClick() {
      if (!this.currentTextbookId) {
        this.$modal.msgWarning('请先选择教材')
        return
      }
      if (!this.currentChapterNode || !this.currentChapterNode.raw) {
        this.$modal.msgWarning('请选中章节点后再新增节')
        return
      }
      const raw = this.currentChapterNode.raw
      if (raw.parentId) {
        this.$modal.msgWarning('请选中章节点后再新增节')
        return
      }
      this.openChapterForm(null, this.currentChapterNode)
    },
    openSubjectForm(row) {
      this.subjectDialogTitle = row ? '修改' : '新增'
      if (row && row.subjectId) {
        getSubject(row.subjectId).then(res => {
          this.subjectForm = Object.assign({}, res.data || row)
          this.subjectOpen = true
        })
      } else {
        this.subjectForm = { subjectName: '', fullScore: 100, orderNum: 0, status: '0', remark: '' }
        this.subjectOpen = true
      }
    },
    submitSubject() {
      this.$refs.subjectFormRef.validate(valid => {
        if (!valid) return
        const req = this.subjectForm.subjectId ? updateSubject(this.subjectForm) : addSubject(this.subjectForm)
        req.then(() => {
          this.$modal.msgSuccess('保存成功')
          this.subjectOpen = false
          this.loadSubjects()
        })
      })
    },
    removeSubject(row) {
      this.$modal.confirm('确认删除？').then(() => delSubject(row.subjectId)).then(() => {
        this.$modal.msgSuccess('删除成功')
        if (this.currentSubjectId === row.subjectId) {
          this.currentSubjectId = undefined
          this.versionList = []
          this.textbookList = []
          this.chapterFlatList = []
        }
        this.loadSubjects()
      }).catch(() => {})
    },
    openVersionForm(row) {
      if (!this.currentSubjectId && !(row && row.versionId)) {
        this.$modal.msgWarning('请先选择学科')
        return
      }
      this.versionDialogTitle = row ? '修改' : '新增'
      if (row && row.versionId) {
        getTextbookVersion(row.versionId).then(res => {
          this.versionForm = Object.assign({}, res.data || row)
          this.versionOpen = true
        })
      } else {
        this.versionForm = { subjectId: this.currentSubjectId, schoolStage: this.currentSchoolStage, versionName: '', orderNum: 0, status: '0' }
        this.versionOpen = true
      }
    },
    submitVersion() {
      this.$refs.versionFormRef.validate(valid => {
        if (!valid) return
        const req = this.versionForm.versionId ? updateTextbookVersion(this.versionForm) : addTextbookVersion(this.versionForm)
        req.then(() => {
          this.$modal.msgSuccess('保存成功')
          this.versionOpen = false
          this.loadVersions()
        })
      })
    },
    removeVersion(row) {
      this.$modal.confirm('确认删除？').then(() => delTextbookVersion(row.versionId)).then(() => {
        this.$modal.msgSuccess('删除成功')
        if (this.currentVersionId === row.versionId) {
          this.currentVersionId = undefined
          this.textbookList = []
          this.chapterFlatList = []
        }
        this.loadVersions()
      }).catch(() => {})
    },
    openTextbookForm(row) {
      if (!this.currentVersionId && !(row && row.textbookId)) {
        this.$modal.msgWarning('请先选择版本')
        return
      }
      this.textbookDialogTitle = row ? '修改' : '新增'
      if (row && row.textbookId) {
        getTextbook(row.textbookId).then(res => {
          this.textbookForm = Object.assign({}, res.data || row)
          this.textbookOpen = true
        })
      } else {
        this.textbookForm = { versionId: this.currentVersionId, textbookName: '', orderNum: 0, status: '0' }
        this.textbookOpen = true
      }
    },
    submitTextbook() {
      this.$refs.textbookFormRef.validate(valid => {
        if (!valid) return
        const req = this.textbookForm.textbookId ? updateTextbook(this.textbookForm) : addTextbook(this.textbookForm)
        req.then(() => {
          this.$modal.msgSuccess('保存成功')
          this.textbookOpen = false
          this.loadTextbooks()
        })
      })
    },
    removeTextbook(row) {
      this.$modal.confirm('确认删除？').then(() => delTextbook(row.textbookId)).then(() => {
        this.$modal.msgSuccess('删除成功')
        if (this.currentTextbookId === row.textbookId) {
          this.currentTextbookId = undefined
          this.chapterFlatList = []
        }
        this.loadTextbooks()
      }).catch(() => {})
    },
    openChapterForm(data, parentNode) {
      if (!this.currentTextbookId && !(data && data.raw)) {
        this.$modal.msgWarning('请先选择教材')
        return
      }
      const row = data && data.raw ? data.raw : null
      const parent = parentNode && parentNode.raw ? parentNode.raw : null
      this.chapterDialogTitle = row ? '修改' : (parent ? '新增节' : '新增章')
      if (row && row.chapterId) {
        getTextbookChapter(row.chapterId).then(res => {
          this.chapterForm = Object.assign({}, res.data || row)
          this.parentChapterLabel = row.parentId ? this.findChapterName(row.parentId) : ''
          this.chapterOpen = true
        })
      } else {
        this.chapterForm = {
          textbookId: this.currentTextbookId,
          parentId: parent ? parent.chapterId : null,
          chapterName: '',
          orderNum: 0
        }
        this.parentChapterLabel = parent ? parent.chapterName : ''
        this.chapterOpen = true
      }
    },
    findChapterName(chapterId) {
      const item = this.chapterFlatList.find(c => c.chapterId === chapterId)
      return item ? item.chapterName : ''
    },
    submitChapter() {
      this.$refs.chapterFormRef.validate(valid => {
        if (!valid) return
        const req = this.chapterForm.chapterId ? updateTextbookChapter(this.chapterForm) : addTextbookChapter(this.chapterForm)
        req.then(() => {
          this.$modal.msgSuccess('保存成功')
          this.chapterOpen = false
          this.loadChapters()
        })
      })
    },
    removeChapter(data) {
      const row = data.raw || data
      this.$modal.confirm('确认删除？').then(() => delTextbookChapter(row.chapterId)).then(() => {
        this.$modal.msgSuccess('删除成功')
        this.loadChapters()
      }).catch(() => {})
    }
  }
}
</script>

<style scoped lang="scss">
.education-page { background: #f4f8fd; min-height: calc(100vh - 84px); }
.page-header { margin-bottom: 16px; }
.page-title { font-size: 24px; font-weight: 700; color: #22324d; }
.page-desc { margin-top: 6px; font-size: 13px; color: #7d8ea8; }
.stage-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
  padding: 10px 14px;
  background: #fff;
  border: 1px solid #e8edf3;
  border-radius: 8px;
}
.stage-bar-label {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}
.catalog-row { margin-bottom: 12px; }
.panel-card {
  background: #fff;
  border: 1px solid #e8edf3;
  border-radius: 8px;
  min-height: 580px;
  display: flex;
  flex-direction: column;
}
.panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 12px;
  border-bottom: 1px solid #eef2f6;
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}
.panel-actions { display: flex; gap: 4px; }
.chapter-tree-wrap {
  flex: 1;
  overflow: auto;
  padding: 8px;
  min-height: 520px;
}
.tree-node {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  padding-right: 4px;
  font-size: 13px;
}
.tree-label { flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.tree-actions { flex-shrink: 0; opacity: 0.85; }
::v-deep .el-tree-node__content { height: 34px; border-radius: 4px; }
::v-deep .el-tree-node.is-current > .el-tree-node__content { background: #ecf5ff; }
</style>
