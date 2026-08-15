<template>

  <div class="app-container library-admin">

    <el-tabs v-model="activeTab" class="admin-tabs" @tab-click="onTabChange">

      <el-tab-pane :label="L.tabAll" name="all" />

      <el-tab-pane :label="L.tabPending" name="pending" />

      <el-tab-pane :label="L.tabShelved" name="shelved" />

      <el-tab-pane :label="L.tabConvertFail" name="convertFail" />

    </el-tabs>



    <el-form v-show="showSearch" ref="queryForm" :model="queryParams" size="small" :inline="true" label-width="80px">

      <el-form-item :label="L.title" prop="keyword">

        <el-input v-model="queryParams.keyword" :placeholder="L.titlePh" clearable @keyup.enter.native="handleQuery" />

      </el-form-item>

      <el-form-item :label="L.subject" prop="subjectId">

        <el-select v-model="queryParams.subjectId" clearable :placeholder="L.subjectPh">

          <el-option v-for="s in subjects" :key="s.subjectId" :label="s.subjectName" :value="s.subjectId" />

        </el-select>

      </el-form-item>

      <el-form-item :label="L.stage" prop="schoolStage">

        <el-select v-model="queryParams.schoolStage" clearable :placeholder="L.stagePh">

          <el-option :label="L.stageJunior" :value="L.stageJunior" />

          <el-option :label="L.stageSenior" :value="L.stageSenior" />

        </el-select>

      </el-form-item>

      <el-form-item :label="L.category" prop="categoryId">

        <el-select v-model="queryParams.categoryId" clearable :placeholder="L.categoryPh">

          <el-option v-for="c in categories" :key="c.categoryId" :label="c.categoryName" :value="c.categoryId" />

        </el-select>

      </el-form-item>

      <el-form-item :label="L.format" prop="fileExtFilter">

        <el-select v-model="queryParams.fileExtFilter" clearable :placeholder="L.formatPh">

          <el-option v-for="ext in extOptions" :key="ext" :label="ext.toUpperCase()" :value="ext" />

        </el-select>

      </el-form-item>

      <el-form-item :label="L.visibility" prop="visibility">

        <el-select v-model="queryParams.visibility" clearable :placeholder="L.visibilityPh">

          <el-option :label="L.visPublic" value="public" />

          <el-option :label="L.visSchool" value="school" />

          <el-option :label="L.visPrivate" value="private" />

        </el-select>

      </el-form-item>

      <el-form-item :label="L.audit" prop="auditStatus">

        <el-select v-model="queryParams.auditStatus" clearable :placeholder="L.auditPh">

          <el-option :label="L.auditPass" value="1" />

          <el-option :label="L.auditReject" value="2" />

          <el-option :label="L.auditPending" value="0" />

        </el-select>

      </el-form-item>

      <el-form-item :label="L.convert" prop="convertStatus">

        <el-select v-model="queryParams.convertStatus" clearable :placeholder="L.convertPh">

          <el-option :label="L.convertSuccess" value="success" />

          <el-option :label="L.convertPending" value="pending" />

          <el-option :label="L.convertFailed" value="failed" />

        </el-select>

      </el-form-item>

      <el-form-item :label="L.uploader" prop="createBy">

        <el-input v-model="queryParams.createBy" clearable :placeholder="L.uploaderPh" @keyup.enter.native="handleQuery" />

      </el-form-item>

      <el-form-item>

        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">{{ L.search }}</el-button>

        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">{{ L.reset }}</el-button>

      </el-form-item>

    </el-form>



    <el-row :gutter="10" class="mb8">

      <el-col :span="1.5">

        <el-button type="success" plain icon="el-icon-check" size="mini" :disabled="multiple" @click="handleAudit('1')" v-hasPermi="['education:library:audit']">{{ L.batchPass }}</el-button>

      </el-col>

      <el-col :span="1.5">

        <el-button type="warning" plain icon="el-icon-close" size="mini" :disabled="multiple" @click="openReject" v-hasPermi="['education:library:audit']">{{ L.batchReject }}</el-button>

      </el-col>

      <el-col :span="1.5">

        <el-button type="primary" plain icon="el-icon-star-on" size="mini" :disabled="multiple" @click="openRecommend" v-hasPermi="['education:library:audit']">{{ L.recommend }}</el-button>

      </el-col>

      <el-col :span="1.5">

        <el-button plain icon="el-icon-star-off" size="mini" :disabled="multiple" @click="handleRecommend('0')" v-hasPermi="['education:library:audit']">{{ L.cancelRecommend }}</el-button>

      </el-col>

      <el-col :span="1.5">

        <el-button type="info" plain icon="el-icon-download" size="mini" :disabled="multiple" @click="handleShelf('1')" v-hasPermi="['education:library:edit']">{{ L.shelfDown }}</el-button>

      </el-col>

      <el-col :span="1.5">

        <el-button type="success" plain icon="el-icon-upload2" size="mini" :disabled="multiple" @click="handleShelf('0')" v-hasPermi="['education:library:edit']">{{ L.shelfUp }}</el-button>

      </el-col>

      <el-col :span="1.5">

        <el-button plain icon="el-icon-refresh" size="mini" :disabled="single" @click="handleReconvert" v-hasPermi="['education:library:edit']">{{ L.reconvert }}</el-button>

      </el-col>

      <el-col :span="1.5">

        <el-button type="danger" plain icon="el-icon-delete" size="mini" :disabled="multiple" @click="handleDelete" v-hasPermi="['education:library:remove']">{{ L.remove }}</el-button>

      </el-col>

      <el-col :span="1.5">

        <el-button plain icon="el-icon-menu" size="mini" @click="goCategory" v-hasPermi="['education:library:category']">{{ L.manageCategory }}</el-button>

      </el-col>

      <el-col :span="1.5">

        <el-button plain icon="el-icon-collection" size="mini" @click="goTopic" v-hasPermi="['education:library:topic']">{{ L.manageTopic }}</el-button>

      </el-col>

      <el-col :span="1.5">

        <el-button plain icon="el-icon-monitor" size="mini" @click="openHealth" v-hasPermi="['education:library:list']">{{ L.healthCheck }}</el-button>

      </el-col>

      <el-col :span="1.5">

        <el-button plain icon="el-icon-medal" size="mini" @click="goVip" v-hasPermi="['education:library:vip']">{{ L.manageVip }}</el-button>

      </el-col>

      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList" />

    </el-row>



    <el-table
      v-loading="loading"
      :data="list"
      class="doc-table"
      :header-cell-style="{ background: '#f8fafc', color: '#475569', fontWeight: 600 }"
      @selection-change="handleSelectionChange"
    >
      <el-table-column type="selection" width="48" align="center" fixed="left" />

      <el-table-column :label="L.colTitle" min-width="260" fixed="left">
        <template slot-scope="scope">
          <div class="doc-cell-title">
            <div class="doc-cell-title__main">
              <span class="doc-ext-tag">{{ (scope.row.fileExt || '-').toUpperCase() }}</span>
              <span class="doc-title-text" :title="scope.row.title">{{ scope.row.title }}</span>
              <i v-if="scope.row.recommendFlag === '1'" class="el-icon-star-on doc-recommend-icon" :title="L.colRecommend" />
            </div>
            <div class="doc-cell-title__meta">
              <span>{{ scope.row.createBy || '-' }}</span>
              <span class="meta-dot">·</span>
              <span>{{ formatDate(scope.row.createTime) }}</span>
              <span class="meta-dot">·</span>
              <span>{{ formatSize(scope.row.fileSize) }}</span>
              <span v-if="formatDocPrice(scope.row.downloadPrice)" class="meta-dot">·</span>
              <span v-if="formatDocPrice(scope.row.downloadPrice)" class="doc-price-tag">{{ formatDocPrice(scope.row.downloadPrice) }}</span>
            </div>
          </div>
        </template>
      </el-table-column>

      <el-table-column :label="L.colCatalog" min-width="150">
        <template slot-scope="scope">
          <div class="doc-cell-catalog">
            <span>{{ catalogLine(scope.row) }}</span>
            <span class="doc-vis-tag">{{ visibilityLabel(scope.row.visibility) }}</span>
          </div>
        </template>
      </el-table-column>

      <el-table-column :label="L.colStatus" width="148" align="center">
        <template slot-scope="scope">
          <div class="doc-status-group">
            <el-tag :type="scope.row.status === '1' ? 'info' : 'success'" size="mini" effect="plain">
              {{ scope.row.status === '1' ? L.shelved : L.online }}
            </el-tag>
            <el-tag
              :type="scope.row.auditStatus === '1' ? 'success' : (scope.row.auditStatus === '2' ? 'danger' : 'warning')"
              size="mini"
              effect="plain"
            >
              {{ auditLabel(scope.row.auditStatus) }}
            </el-tag>
            <el-tooltip v-if="scope.row.previewError" :content="scope.row.previewError" placement="top">
              <el-tag
                v-if="scope.row.convertStatus"
                :type="scope.row.convertStatus === 'success' ? 'success' : (scope.row.convertStatus === 'failed' ? 'danger' : 'warning')"
                size="mini"
                effect="plain"
              >
                {{ convertLabel(scope.row.convertStatus) }}
              </el-tag>
            </el-tooltip>
            <el-tag
              v-else-if="scope.row.convertStatus"
              :type="scope.row.convertStatus === 'success' ? 'success' : (scope.row.convertStatus === 'failed' ? 'danger' : 'warning')"
              size="mini"
              effect="plain"
            >
              {{ convertLabel(scope.row.convertStatus) }}
            </el-tag>
          </div>
        </template>
      </el-table-column>

      <el-table-column :label="L.colStats" width="96" align="center">
        <template slot-scope="scope">
          <div class="doc-stats">
            <span><i class="el-icon-view" />{{ scope.row.viewCount || 0 }}</span>
            <span><i class="el-icon-download" />{{ scope.row.downloadCount || 0 }}</span>
          </div>
        </template>
      </el-table-column>

      <el-table-column :label="L.colAction" width="148" align="center" fixed="right">
        <template slot-scope="scope">
          <div class="doc-actions">
            <el-button size="mini" type="text" icon="el-icon-view" @click="openPortal(scope.row)">{{ L.preview }}</el-button>
            <el-button size="mini" type="text" icon="el-icon-document" @click="openDetail(scope.row)">{{ L.detail }}</el-button>
            <el-dropdown trigger="click" @command="cmd => handleRowCommand(cmd, scope.row)">
              <el-button size="mini" type="text" icon="el-icon-more">{{ L.more }}</el-button>
              <el-dropdown-menu slot="dropdown">
                <el-dropdown-item command="edit" icon="el-icon-edit" v-hasPermi="['education:library:edit']">{{ L.edit }}</el-dropdown-item>
                <el-dropdown-item
                  v-if="scope.row.status !== '1'"
                  command="shelfDown"
                  icon="el-icon-download"
                  v-hasPermi="['education:library:edit']"
                >{{ L.shelfDown }}</el-dropdown-item>
                <el-dropdown-item
                  v-else
                  command="shelfUp"
                  icon="el-icon-upload2"
                  v-hasPermi="['education:library:edit']"
                >{{ L.shelfUp }}</el-dropdown-item>
              </el-dropdown-menu>
            </el-dropdown>
          </div>
        </template>
      </el-table-column>
    </el-table>



    <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />



    <el-dialog :title="L.rejectTitle" :visible.sync="rejectOpen" width="420px" append-to-body>

      <el-input v-model="rejectRemark" type="textarea" :rows="4" :placeholder="L.rejectPh" />

      <div slot="footer">

        <el-button @click="rejectOpen = false">{{ L.cancel }}</el-button>

        <el-button type="primary" @click="confirmReject">{{ L.confirm }}</el-button>

      </div>

    </el-dialog>



    <el-dialog :title="L.recommendTitle" :visible.sync="recommendOpen" width="420px" append-to-body>

      <el-form label-width="96px" size="small">

        <el-form-item :label="L.recommendOrder">

          <el-input-number v-model="recommendOrder" :min="0" :max="9999" controls-position="right" style="width: 100%" />

        </el-form-item>

        <p class="form-hint">{{ L.recommendHint }}</p>

      </el-form>

      <div slot="footer">

        <el-button @click="recommendOpen = false">{{ L.cancel }}</el-button>

        <el-button type="primary" @click="confirmRecommend">{{ L.confirm }}</el-button>

      </div>

    </el-dialog>



    <el-dialog :title="L.editTitle" :visible.sync="editOpen" width="680px" append-to-body>

      <el-form ref="editForm" :model="editForm" label-width="96px" size="small">

        <el-form-item :label="L.colTitle">

          <el-input v-model="editForm.title" maxlength="200" />

        </el-form-item>

        <el-form-item :label="L.fieldSummary">

          <el-input v-model="editForm.summary" type="textarea" :rows="3" maxlength="500" />

        </el-form-item>

        <el-form-item :label="L.fieldTags">

          <el-input v-model="editForm.tagNames" :placeholder="L.fieldTagsPh" />

        </el-form-item>

        <el-form-item :label="L.fieldCover">

          <image-upload v-model="editForm.coverUrl" :limit="1" :file-size="5" />

        </el-form-item>

        <el-form-item :label="L.stage">

          <el-select v-model="editCatalogForm.schoolStage" clearable style="width: 100%" @change="onEditStageChange">

            <el-option :label="L.stageJunior" :value="L.stageJunior" />

            <el-option :label="L.stageSenior" :value="L.stageSenior" />

          </el-select>

        </el-form-item>

        <el-row :gutter="16">

          <el-col :span="12">

            <el-form-item :label="L.subject">

              <el-select v-model="editForm.subjectId" clearable style="width: 100%" @change="onEditSubjectChange">

                <el-option v-for="s in subjects" :key="s.subjectId" :label="s.subjectName" :value="s.subjectId" />

              </el-select>

            </el-form-item>

          </el-col>

          <el-col :span="12">

            <el-form-item :label="L.category">

              <el-select v-model="editForm.categoryId" clearable style="width: 100%">

                <el-option v-for="c in categories" :key="c.categoryId" :label="c.categoryName" :value="c.categoryId" />

              </el-select>

            </el-form-item>

          </el-col>

        </el-row>

        <document-catalog-fields v-model="editCatalogForm" :subject-id="editForm.subjectId" hide-stage />

        <el-form-item :label="L.colVisibility">

          <el-radio-group v-model="editForm.visibility">

            <el-radio label="public">{{ L.visPublic }}</el-radio>

            <el-radio label="school">{{ L.visSchool }}</el-radio>

            <el-radio label="private">{{ L.visPrivate }}</el-radio>

          </el-radio-group>

        </el-form-item>

        <el-form-item :label="L.fieldDownload">

          <el-switch v-model="editForm.allowDownload" active-value="1" inactive-value="0" />

        </el-form-item>

        <el-form-item :label="L.downloadPrice">

          <el-input-number v-model="editForm.downloadPrice" :min="0" :max="9999" :precision="2" :step="0.5" controls-position="right" />

          <p class="preview-settings-hint">{{ L.downloadPriceHint }}</p>

        </el-form-item>

      </el-form>

      <div slot="footer">

        <el-button @click="editOpen = false">{{ L.cancel }}</el-button>

        <el-button type="primary" :loading="editing" @click="submitEdit">{{ L.save }}</el-button>

      </div>

    </el-dialog>



    <el-drawer :title="L.detailTitle" :visible.sync="detailOpen" size="480px" append-to-body>

      <div v-if="detailRow" class="detail-drawer">

        <dl>

          <dt>{{ L.colTitle }}</dt><dd>{{ detailRow.title }}</dd>

          <dt>{{ L.colFormat }}</dt><dd>{{ (detailRow.fileExt || '').toUpperCase() }}</dd>

          <dt>{{ L.colCatalog }}</dt><dd>{{ catalogLine(detailRow) }} / {{ visibilityLabel(detailRow.visibility) }}</dd>

          <dt>{{ L.downloadPrice }}</dt><dd>{{ formatDocPrice(detailRow.downloadPrice) || L.free }}</dd>

          <dt>{{ L.colRecommend }}</dt><dd>{{ detailRow.recommendFlag === '1' ? (L.yes + '(' + (detailRow.recommendOrder || 0) + ')') : '-' }}</dd>

          <dt>{{ L.colSize }}</dt><dd>{{ formatSize(detailRow.fileSize) }}</dd>

          <dt>{{ L.colShelf }}</dt><dd>{{ detailRow.status === '1' ? L.shelved : L.online }}</dd>

          <dt>{{ L.colAudit }}</dt><dd>{{ auditLabel(detailRow.auditStatus) }}</dd>

          <dt>{{ L.colConvert }}</dt><dd>{{ convertLabel(detailRow.convertStatus) }}</dd>

          <dt v-if="detailRow.previewError">{{ L.previewError }}</dt><dd v-if="detailRow.previewError" class="text-danger">{{ detailRow.previewError }}</dd>

          <dt>{{ L.colUploader }}</dt><dd>{{ detailRow.createBy }}</dd>

          <dt>{{ L.colStats }}</dt><dd>{{ L.colViews }} {{ detailRow.viewCount || 0 }} / {{ L.colDownloads }} {{ detailRow.downloadCount || 0 }}</dd>

          <dt>{{ L.colTime }}</dt><dd>{{ detailRow.createTime }}</dd>

          <dt v-if="detailRow.auditBy">{{ L.auditBy }}</dt><dd v-if="detailRow.auditBy">{{ detailRow.auditBy }} / {{ detailRow.auditTime || '-' }}</dd>

          <dt v-if="detailRow.auditRemark">{{ L.auditRemark }}</dt><dd v-if="detailRow.auditRemark">{{ detailRow.auditRemark }}</dd>

          <dt>{{ L.fileUrl }}</dt><dd class="mono">{{ detailRow.fileUrl }}</dd>

          <dt v-if="detailRow.previewUrl">{{ L.previewUrl }}</dt><dd v-if="detailRow.previewUrl" class="mono">{{ detailRow.previewUrl }}</dd>

          <dt>{{ L.fileStorage }}</dt><dd>{{ detailRow.fileStorage || '-' }}</dd>

        </dl>

      </div>

    </el-drawer>



    <el-dialog :title="L.healthTitle" :visible.sync="healthOpen" width="560px" append-to-body @open="loadHealth">

      <div v-loading="healthLoading">

        <el-descriptions v-if="healthData" :column="1" border size="small">

          <el-descriptions-item :label="L.healthStorage">{{ healthData.storageType || '-' }}</el-descriptions-item>

          <el-descriptions-item :label="L.healthLocalRoot">

            <el-tag :type="healthData.localFileRootOk ? 'success' : 'danger'" size="mini">{{ healthData.localFileRootOk ? L.healthOk : L.healthFail }}</el-tag>

            <span class="health-path">{{ healthData.localFileRoot || '-' }}</span>

          </el-descriptions-item>

          <el-descriptions-item :label="L.healthLibreOffice">

            <el-tag :type="healthData.libreOfficeOk ? 'success' : 'danger'" size="mini">{{ healthData.libreOfficeOk ? L.healthOk : L.healthFail }}</el-tag>

            <span class="health-path">{{ healthData.libreOfficeHome || '-' }}</span>

          </el-descriptions-item>

          <el-descriptions-item :label="L.healthKkfileview">

            <el-tag :type="healthData.kkfileviewOk ? 'success' : 'warning'" size="mini">{{ healthData.kkfileviewOk ? L.healthOk : L.healthWarn }}</el-tag>

            <span class="health-path">{{ healthData.kkfileviewBaseUrl || '-' }}</span>

          </el-descriptions-item>

          <el-descriptions-item :label="L.healthPublicUrl">{{ healthData.filePublicBaseUrl || '-' }}</el-descriptions-item>

          <el-descriptions-item :label="L.healthAudit">{{ healthData.auditEnabled ? L.healthOn : L.healthOff }}</el-descriptions-item>

        </el-descriptions>



        <el-divider>{{ L.previewSettings }}</el-divider>

        <el-form label-width="120px" size="small" class="preview-settings-form">

          <el-form-item :label="L.previewMaxPages">

            <el-input-number v-model="previewMaxPages" :min="1" :max="100" controls-position="right" />

            <p class="preview-settings-hint">{{ L.previewMaxPagesHint }}</p>

          </el-form-item>

        </el-form>



        <el-divider>{{ L.paySettings }}</el-divider>

        <el-form label-width="120px" size="small" class="preview-settings-form">

          <el-form-item :label="L.payEnabled">

            <el-switch v-model="payForm.enabled" />

          </el-form-item>

          <el-form-item :label="L.payPid">

            <el-input v-model="payForm.pid" placeholder="ZPay 商户 PID" />

          </el-form-item>

          <el-form-item :label="L.payKey">

            <el-input v-model="payForm.key" :placeholder="payForm.keyConfigured ? payForm.keyMasked : '商户密钥'" show-password />

          </el-form-item>

          <el-form-item :label="L.payGateway">

            <el-input v-model="payForm.gatewayUrl" placeholder="https://zpayz.cn" />

          </el-form-item>

          <el-form-item :label="L.payNotify">

            <el-input v-model="payForm.notifyUrl" placeholder="http://网关地址/system/education/pay/zpay/notify" />

          </el-form-item>

          <el-form-item :label="L.paperExportFee">

            <el-input-number v-model="payForm.paperExportFee" :min="0" :max="9999" :precision="2" :step="0.5" controls-position="right" />

            <p class="preview-settings-hint">{{ L.paperExportFeeHint }}</p>

          </el-form-item>

        </el-form>



        <el-divider>{{ L.vipSettings }}</el-divider>

        <el-form label-width="120px" size="small" class="preview-settings-form">

          <el-form-item :label="L.vipEnabled">

            <el-switch v-model="vipForm.enabled" />

          </el-form-item>

          <el-form-item :label="L.vipPrice">

            <el-input-number v-model="vipForm.price" :min="0" :max="9999" :precision="2" :step="1" controls-position="right" />

          </el-form-item>

          <el-form-item :label="L.vipDuration">

            <el-input-number v-model="vipForm.durationDays" :min="1" :max="3650" controls-position="right" />

          </el-form-item>

          <el-form-item :label="L.vipFreeDownload">

            <el-switch v-model="vipForm.freeDownload" />

          </el-form-item>

          <el-form-item :label="L.vipPreviewPages">

            <el-input-number v-model="vipForm.previewPages" :min="0" :max="100" controls-position="right" />

            <p class="preview-settings-hint">{{ L.vipPreviewPagesHint }}</p>

          </el-form-item>

        </el-form>



        <el-divider v-if="healthStats">{{ L.healthStats }}</el-divider>



        <el-row v-if="healthStats" :gutter="12" class="health-stats">

          <el-col :span="8"><div class="stat-card"><div class="stat-num">{{ healthStats.totaldocs || healthStats.totalDocs || 0 }}</div><div class="stat-label">{{ L.statTotal }}</div></div></el-col>

          <el-col :span="8"><div class="stat-card"><div class="stat-num">{{ healthStats.pendingaudit || healthStats.pendingAudit || 0 }}</div><div class="stat-label">{{ L.statPending }}</div></div></el-col>

          <el-col :span="8"><div class="stat-card"><div class="stat-num">{{ healthStats.convertfailed || healthStats.convertFailed || 0 }}</div><div class="stat-label">{{ L.statConvertFail }}</div></div></el-col>

          <el-col :span="8"><div class="stat-card"><div class="stat-num">{{ healthStats.shelveddocs || healthStats.shelvedDocs || 0 }}</div><div class="stat-label">{{ L.statShelved }}</div></div></el-col>

          <el-col :span="8"><div class="stat-card"><div class="stat-num">{{ healthStats.convertpending || healthStats.convertPending || 0 }}</div><div class="stat-label">{{ L.statConvertPending }}</div></div></el-col>

          <el-col :span="8"><div class="stat-card"><div class="stat-num">{{ healthStats.categorycount || healthStats.categoryCount || 0 }}</div><div class="stat-label">{{ L.statCategory }}</div></div></el-col>

        </el-row>

      </div>

      <div slot="footer">

        <el-button @click="healthOpen = false">{{ L.cancel }}</el-button>

        <el-button type="success" :loading="previewSettingsSaving" @click="savePreviewSettings">{{ L.previewSave }}</el-button>

        <el-button type="warning" :loading="paySettingsSaving" @click="savePaySettings">{{ L.paySave }}</el-button>

        <el-button type="success" :loading="vipSettingsSaving" @click="saveVipSettings">{{ L.vipSave }}</el-button>

        <el-button type="primary" :loading="healthLoading" @click="loadHealth">{{ L.healthRefresh }}</el-button>

      </div>

    </el-dialog>

  </div>

</template>



<script>

import { subjectOptions } from '@/api/education/subject'

import {

  listLibraryDocuments,

  delLibraryDocument,

  auditLibraryDocuments,

  listLibraryCategories,

  recommendLibraryDocuments,

  reconvertLibraryDocument,

  updateLibraryDocument,

  changeLibraryDocumentStatus,

  getLibraryAdminHealth,

  getLibraryAdminSettings,

  updateLibraryAdminSettings

} from '@/api/education/library'

import { getPayAdminSettings, updatePayAdminSettings } from '@/api/education/pay'
import { getVipAdminConfig, updateVipAdminConfig } from '@/api/education/vip'

import DocumentCatalogFields from '@/views/portal/library/components/DocumentCatalogFields'
import { LIBRARY_UPLOAD_EXTS } from '@/utils/libraryFileExt'



function emptyCatalogForm() {

  return {

    schoolStage: '\u9ad8\u4e2d',

    versionId: undefined,

    textbookId: undefined,

    chapterPath: [],

    chapterId: undefined,

    chapterText: ''

  }

}



const L = {

  tabAll: '\u5168\u90e8\u6587\u6863',

  tabPending: '\u5f85\u5ba1\u6838',

  tabShelved: '\u5df2\u4e0b\u67b6',

  tabConvertFail: '\u8f6c\u6362\u5931\u8d25',

  title: '\u6807\u9898',

  titlePh: '\u641c\u7d22\u6807\u9898/\u6807\u7b7e',

  subject: '\u5b66\u79d1',

  subjectPh: '\u5b66\u79d1',

  stage: '\u5b66\u6bb5',

  stagePh: '\u5b66\u6bb5',

  stageJunior: '\u521d\u4e2d',

  stageSenior: '\u9ad8\u4e2d',

  category: '\u5206\u7c7b',

  categoryPh: '\u5206\u7c7b',

  format: '\u683c\u5f0f',

  formatPh: '\u683c\u5f0f',

  visibility: '\u53ef\u89c1\u8303\u56f4',

  visibilityPh: '\u53ef\u89c1\u8303\u56f4',

  audit: '\u5ba1\u6838',

  auditPh: '\u5ba1\u6838\u72b6\u6001',

  auditPass: '\u901a\u8fc7',

  auditReject: '\u9a73\u56de',

  auditPending: '\u5f85\u5ba1',

  convert: '\u8f6c\u6362',

  convertPh: '\u8f6c\u6362\u72b6\u6001',

  convertSuccess: '\u6210\u529f',

  convertPending: '\u8f6c\u6362\u4e2d',

  convertFailed: '\u5931\u8d25',

  uploader: '\u4e0a\u4f20\u8005',

  uploaderPh: '\u7528\u6237\u540d',

  search: '\u641c\u7d22',

  reset: '\u91cd\u7f6e',

  remove: '\u5220\u9664',

  batchPass: '\u6279\u91cf\u901a\u8fc7',

  batchReject: '\u6279\u91cf\u9a73\u56de',

  recommend: '\u8bbe\u4e3a\u63a8\u8350',

  cancelRecommend: '\u53d6\u6d88\u63a8\u8350',

  recommendTitle: '\u8bbe\u7f6e\u63a8\u8350',

  recommendOrder: '\u6392\u5e8f\u503c',

  recommendHint: '\u6570\u503c\u8d8a\u5927\u6392\u5e8f\u8d8a\u9760\u524d\uff0c\u5efa\u8bae 100 \u8d77',

  shelfDown: '\u4e0b\u67b6',

  shelfUp: '\u6062\u590d\u4e0a\u67b6',

  shelved: '\u5df2\u4e0b\u67b6',

  online: '\u4e0a\u67b6',

  reconvert: '\u91cd\u65b0\u8f6c\u6362',

  colRecommend: '\u63a8\u8350',

  colShelf: '\u72b6\u6001',

  colConvert: '\u8f6c\u6362',

  yes: '\u662f',

  colTitle: '\u6807\u9898',

  colCatalog: '\u5206\u7c7b\u4fe1\u606f',

  colStatus: '\u72b6\u6001',

  colStats: '\u6570\u636e',

  colFormat: '\u683c\u5f0f',

  colSubject: '\u5b66\u79d1',

  colCategory: '\u5206\u7c7b',

  colVisibility: '\u53ef\u89c1\u8303\u56f4',

  colAudit: '\u5ba1\u6838',

  colViews: '\u9605\u8bfb',

  colDownloads: '\u4e0b\u8f7d',

  colSize: '\u5927\u5c0f',

  colUploader: '\u4e0a\u4f20\u8005',

  colTime: '\u4e0a\u4f20\u65f6\u95f4',

  colAction: '\u64cd\u4f5c',

  more: '\u66f4\u591a',

  preview: '\u9884\u89c8',

  detail: '\u8be6\u60c5',

  edit: '\u7f16\u8f91',

  editTitle: '\u7f16\u8f91\u6587\u6863',

  detailTitle: '\u6587\u6863\u8be6\u60c5',

  fieldSummary: '\u7b80\u4ecb',

  fieldTags: '\u6807\u7b7e',

  fieldTagsPh: '\u591a\u4e2a\u6807\u7b7e\u7528\u9017\u53f7\u5206\u9694',

  fieldCover: '\u5c01\u9762',

  fieldDownload: '\u5141\u8bb8\u4e0b\u8f7d',

  save: '\u4fdd\u5b58',

  rejectTitle: '\u9a73\u56de\u539f\u56e0',

  rejectPh: '\u8bf7\u586b\u5199\u9a73\u56de\u539f\u56e0',

  cancel: '\u53d6\u6d88',

  confirm: '\u786e\u5b9a',

  visPublic: '\u516c\u5f00',

  visSchool: '\u6821\u5185',

  visPrivate: '\u4ec5\u672c\u4eba',

  auditBy: '\u5ba1\u6838\u4eba',

  auditRemark: '\u5ba1\u6838\u5907\u6ce8',

  previewError: '\u9884\u89c8\u9519\u8bef',

  fileUrl: '\u6587\u4ef6\u5730\u5740',

  previewUrl: '\u9884\u89c8\u5730\u5740',

  fileStorage: '\u5b58\u50a8\u7c7b\u578b',

  manageCategory: '\u5206\u7c7b\u7ba1\u7406',
  manageTopic: '\u70ed\u95e8\u4e13\u9898',

  healthCheck: '\u5065\u5eb7\u68c0\u67e5',

  healthTitle: '\u6587\u5e93\u670d\u52a1\u5065\u5eb7',

  healthStorage: '\u5b58\u50a8\u7c7b\u578b',

  healthLocalRoot: '\u672c\u5730\u6587\u4ef6\u76ee\u5f55',

  healthLibreOffice: 'LibreOffice',

  healthKkfileview: 'kkFileView',

  healthPublicUrl: '\u6587\u4ef6\u516c\u7f51\u5730\u5740',

  healthAudit: '\u5ba1\u6838\u5f00\u5173',

  healthOk: '\u6b63\u5e38',

  healthFail: '\u5f02\u5e38',

  healthWarn: '\u672a\u914d\u7f6e',

  healthOn: '\u5f00\u542f',

  healthOff: '\u5173\u95ed',

  healthStats: '\u6570\u636e\u6982\u89c8',

  healthRefresh: '\u5237\u65b0',

  previewSettings: '\u9884\u89c8\u8bbe\u7f6e',

  previewMaxPages: '\u9884\u89c8\u9875\u6570\u4e0a\u9650',

  previewMaxPagesHint: '\u95e8\u6237\u5728\u7ebf\u9884\u89c8\u6700\u591a\u663e\u793a\u9875\u6570\uff0c\u8d85\u51fa\u90e8\u5206\u9700\u4e0b\u8f7d\u67e5\u770b',

  previewSave: '\u4fdd\u5b58\u8bbe\u7f6e',

  previewSaveOk: '\u9884\u89c8\u8bbe\u7f6e\u5df2\u4fdd\u5b58',

  downloadPrice: '\u4e0b\u8f7d\u4ef7\u683c\uff08\u5143\uff09',

  free: '\u514d\u8d39',

  downloadPriceHint: '0 \u8868\u793a\u514d\u8d39\u4e0b\u8f7d\uff1b\u542f\u7528\u652f\u4ed8\u540e\u7528\u6237\u9700\u4ed8\u8d39\u624d\u53ef\u4e0b\u8f7d',

  paySettings: 'ZPay \u652f\u4ed8\u8bbe\u7f6e',

  payEnabled: '\u542f\u7528\u652f\u4ed8',

  payPid: '\u5546\u6237 PID',

  payKey: '\u5546\u6237\u5bc6\u94a5',

  payGateway: '\u652f\u4ed8\u7f51\u5173',

  payNotify: '\u5f02\u6b65\u56de\u8c03',

  paperExportFee: '\u7ec4\u5377\u5bfc\u51fa\u8d39\u7528',

  paperExportFeeHint: '0 \u8868\u793a\u514d\u8d39\u5bfc\u51fa\u8bd5\u5377',

  paySave: '\u4fdd\u5b58\u652f\u4ed8\u8bbe\u7f6e',

  paySaveOk: '\u652f\u4ed8\u8bbe\u7f6e\u5df2\u4fdd\u5b58',
  manageVip: 'VIP \u4f1a\u5458\u7ba1\u7406',
  vipSettings: 'VIP \u4f1a\u5458\u8bbe\u7f6e',
  vipEnabled: '\u5f00\u542f VIP',
  vipPrice: 'VIP \u4ef7\u683c\uff08\u5143\uff09',
  vipDuration: '\u6709\u6548\u5929\u6570',
  vipFreeDownload: '\u514d\u8d39\u4e0b\u8f7d\u4ed8\u8d39\u6587\u6863',
  vipPreviewPages: 'VIP \u9884\u89c8\u9875\u6570',
  vipPreviewPagesHint: '0 \u8868\u793a\u4f7f\u7528\u9ed8\u8ba4\u9884\u89c8\u9875\u6570\u4e0a\u9650',
  vipSave: '\u4fdd\u5b58 VIP \u8bbe\u7f6e',
  vipSaveOk: 'VIP \u8bbe\u7f6e\u5df2\u4fdd\u5b58',

  statTotal: '\u6587\u6863\u603b\u6570',

  statPending: '\u5f85\u5ba1\u6838',

  statConvertFail: '\u8f6c\u6362\u5931\u8d25',

  statShelved: '\u5df2\u4e0b\u67b6',

  statConvertPending: '\u8f6c\u6362\u4e2d',

  statCategory: '\u5206\u7c7b\u6570'

}



export default {

  name: 'EduLibraryAdmin',

  components: { DocumentCatalogFields },

  data() {

    return {

      L,

      activeTab: 'all',

      loading: false,

      showSearch: true,

      ids: [],

      single: true,

      multiple: true,

      total: 0,

      list: [],

      subjects: [],

      categories: [],

      extOptions: LIBRARY_UPLOAD_EXTS,

      rejectOpen: false,

      rejectRemark: '',

      recommendOpen: false,

      recommendOrder: 100,

      editOpen: false,

      editing: false,

      editForm: {},

      editCatalogForm: emptyCatalogForm(),

      detailOpen: false,

      detailRow: null,

      healthOpen: false,

      healthLoading: false,

      healthData: null,

      previewMaxPages: 5,

      previewSettingsSaving: false,

      payForm: {
        enabled: false,
        pid: '',
        key: '',
        keyConfigured: false,
        keyMasked: '',
        gatewayUrl: 'https://zpayz.cn',
        notifyUrl: '',
        paperExportFee: 0
      },

      paySettingsSaving: false,

      vipForm: {
        enabled: false,
        price: 29,
        durationDays: 30,
        freeDownload: true,
        previewPages: 0
      },

      vipSettingsSaving: false,

      queryParams: {

        pageNum: 1,

        pageSize: 10,

        keyword: undefined,

        subjectId: undefined,

        schoolStage: undefined,

        categoryId: undefined,

        fileExtFilter: undefined,

        visibility: undefined,

        auditStatus: undefined,

        convertStatus: undefined,

        status: undefined,

        createBy: undefined

      }

    }

  },

  computed: {

    healthStats() {

      return this.healthData && this.healthData.stats ? this.healthData.stats : null

    }

  },

  created() {

    subjectOptions().then(res => { this.subjects = res.data || [] }).catch(() => { this.subjects = [] })

    listLibraryCategories().then(res => { this.categories = res.data || [] })

    this.getList()

  },

  methods: {

    visibilityLabel(v) {

      if (v === 'public') return L.visPublic

      if (v === 'private') return L.visPrivate

      return L.visSchool

    },

    auditLabel(v) {

      if (v === '1') return L.auditPass

      if (v === '2') return L.auditReject

      return L.auditPending

    },

    convertLabel(v) {

      if (v === 'success') return L.convertSuccess

      if (v === 'failed') return L.convertFailed

      if (v === 'pending') return L.convertPending

      return '-'

    },

    formatSize(size) {

      const n = Number(size) || 0

      if (n < 1024) return n + ' B'

      if (n < 1024 * 1024) return (n / 1024).toFixed(1) + ' KB'

      return (n / 1024 / 1024).toFixed(1) + ' MB'

    },

    formatDate(value) {

      if (!value) return '-'

      return String(value).replace('T', ' ').slice(0, 16)

    },

    formatDocPrice(price) {

      const n = Number(price)

      if (!Number.isFinite(n) || n <= 0) return ''

      return '\u00a5' + n.toFixed(2)

    },

    catalogLine(row) {

      const parts = [row.subjectName, row.schoolStage, row.categoryName].filter(Boolean)

      return parts.length ? parts.join(' · ') : '-'

    },

    handleRowCommand(command, row) {

      if (command === 'edit') {

        this.openEdit(row)

      } else if (command === 'shelfDown') {

        this.handleShelf('1', row)

      } else if (command === 'shelfUp') {

        this.handleShelf('0', row)

      }

    },

    onTabChange() {

      this.queryParams.auditStatus = undefined

      this.queryParams.status = undefined

      this.queryParams.convertStatus = undefined

      if (this.activeTab === 'pending') {

        this.queryParams.auditStatus = '0'

      } else if (this.activeTab === 'shelved') {

        this.queryParams.status = '1'

      } else if (this.activeTab === 'convertFail') {

        this.queryParams.convertStatus = 'failed'

      }

      this.handleQuery()

    },

    getList() {

      this.loading = true

      listLibraryDocuments(this.queryParams).then(res => {

        this.list = res.rows || []

        this.total = res.total || 0

      }).finally(() => { this.loading = false })

    },

    handleQuery() {

      this.queryParams.pageNum = 1

      this.getList()

    },

    resetQuery() {

      const tab = this.activeTab

      this.resetForm('queryForm')

      this.activeTab = tab

      this.onTabChange()

    },

    handleSelectionChange(selection) {

      this.ids = selection.map(item => item.documentId)

      this.single = selection.length !== 1

      this.multiple = !selection.length

    },

    handleDelete(row) {

      const ids = row && row.documentId ? row.documentId : this.ids.join(',')

      this.$modal.confirm('\u786e\u8ba4\u5220\u9664\u9009\u4e2d\u6587\u6863\uff1f\u5220\u9664\u540e\u4e0d\u53ef\u6062\u590d\u3002').then(() => delLibraryDocument(ids)).then(() => {

        this.getList()

        this.$modal.msgSuccess('\u5220\u9664\u6210\u529f')

      }).catch(() => {})

    },

    handleAudit(status, remark) {

      if (!this.ids.length) return

      auditLibraryDocuments({

        documentIds: this.ids,

        auditStatus: status,

        auditRemark: remark

      }).then(() => {

        this.getList()

        this.$modal.msgSuccess('\u5ba1\u6838\u5b8c\u6210')

      })

    },

    openReject() {

      this.rejectRemark = ''

      this.rejectOpen = true

    },

    confirmReject() {

      this.rejectOpen = false

      this.handleAudit('2', this.rejectRemark)

    },

    openRecommend() {

      if (!this.ids.length) return

      this.recommendOrder = 100

      this.recommendOpen = true

    },

    confirmRecommend() {

      this.recommendOpen = false

      recommendLibraryDocuments({

        documentIds: this.ids,

        recommendFlag: '1',

        recommendOrder: this.recommendOrder

      }).then(() => {

        this.getList()

        this.$modal.msgSuccess('\u64cd\u4f5c\u6210\u529f')

      })

    },

    handleRecommend(flag) {

      if (!this.ids.length) return

      recommendLibraryDocuments({

        documentIds: this.ids,

        recommendFlag: flag,

        recommendOrder: flag === '1' ? 100 : 0

      }).then(() => {

        this.getList()

        this.$modal.msgSuccess('\u64cd\u4f5c\u6210\u529f')

      })

    },

    handleShelf(status, row) {

      const ids = row && row.documentId ? [row.documentId] : this.ids

      if (!ids.length) return

      const msg = status === '1' ? '\u786e\u8ba4\u4e0b\u67b6\u9009\u4e2d\u6587\u6863\uff1f' : '\u786e\u8ba4\u6062\u590d\u4e0a\u67b6\u9009\u4e2d\u6587\u6863\uff1f'

      this.$modal.confirm(msg).then(() => changeLibraryDocumentStatus({ documentIds: ids, status })).then(() => {

        this.getList()

        this.$modal.msgSuccess('\u64cd\u4f5c\u6210\u529f')

      }).catch(() => {})

    },

    handleReconvert() {

      if (this.ids.length !== 1) return

      reconvertLibraryDocument(this.ids[0]).then(() => {

        this.$modal.msgSuccess('\u5df2\u63d0\u4ea4\u8f6c\u6362\u4efb\u52a1\uff0c\u538b\u7f29\u5305\u9700\u7b49\u5f85\u5185\u5d4c\u6587\u6863\u9884\u8f6c\u6362\u5b8c\u6210')

        this.getList()

      })

    },

    openPortal(row) {

      if (!row || !row.documentId) return

      window.open('/library/' + row.documentId, '_blank')

    },

    openDetail(row) {

      this.detailRow = row

      this.detailOpen = true

    },

    goCategory() {

      this.$router.push({ path: '/admin/question-bank-center/libraryCategory' })

    },

    goTopic() {

      this.$router.push({ path: '/admin/question-bank-center/libraryTopic' })

    },

    goVip() {

      this.$router.push({ path: '/admin/question-bank-center/libraryVip' })

    },

    openHealth() {

      this.healthOpen = true

      this.loadPaySettings()

      this.loadVipSettings()

    },

    loadHealth() {

      this.healthLoading = true

      getLibraryAdminHealth().then(res => {

        this.healthData = res.data || null

        if (this.healthData && this.healthData.previewMaxPages) {

          this.previewMaxPages = Number(this.healthData.previewMaxPages) || 5

        }

      }).finally(() => { this.healthLoading = false })

    },

    savePreviewSettings() {

      this.previewSettingsSaving = true

      updateLibraryAdminSettings({ previewMaxPages: this.previewMaxPages }).then(() => {

        this.$modal.msgSuccess(L.previewSaveOk)

        this.loadHealth()

      }).finally(() => { this.previewSettingsSaving = false })

    },

    loadPaySettings() {

      getPayAdminSettings().then(res => {

        const data = res.data || {}

        this.payForm = {

          enabled: !!data.enabled,

          pid: data.pid || '',

          key: '',

          keyConfigured: !!data.keyConfigured,

          keyMasked: data.keyMasked || '',

          gatewayUrl: data.gatewayUrl || 'https://zpayz.cn',

          notifyUrl: data.notifyUrl || '',

          paperExportFee: Number(data.paperExportFee) || 0

        }

      }).catch(() => {})

    },

    loadVipSettings() {

      getVipAdminConfig().then(res => {

        const data = res.data || {}

        this.vipForm = {

          enabled: !!data.enabled,

          price: Number(data.price) || 0,

          durationDays: Number(data.durationDays) || 30,

          freeDownload: data.freeDownload !== false,

          previewPages: Number(data.previewPages) || 0

        }

      }).catch(() => {})

    },

    saveVipSettings() {

      this.vipSettingsSaving = true

      updateVipAdminConfig(this.vipForm).then(() => {

        this.$modal.msgSuccess(L.vipSaveOk)

      }).finally(() => { this.vipSettingsSaving = false })

    },

    savePaySettings() {

      this.paySettingsSaving = true

      updatePayAdminSettings({

        enabled: this.payForm.enabled,

        pid: this.payForm.pid,

        key: this.payForm.key,

        gatewayUrl: this.payForm.gatewayUrl,

        notifyUrl: this.payForm.notifyUrl,

        paperExportFee: this.payForm.paperExportFee

      }).then(() => {

        this.$modal.msgSuccess(L.paySaveOk)

        this.loadPaySettings()

      }).finally(() => { this.paySettingsSaving = false })

    },

    openEdit(row) {

      this.editForm = {

        documentId: row.documentId,

        title: row.title,

        summary: row.summary,

        tagNames: row.tagNames,

        coverUrl: row.coverUrl || '',

        subjectId: row.subjectId,

        categoryId: row.categoryId,

        visibility: row.visibility || 'school',

        allowDownload: row.allowDownload || '1',

        downloadPrice: Number(row.downloadPrice) || 0

      }

      this.editCatalogForm = {

        schoolStage: row.schoolStage || '\u9ad8\u4e2d',

        versionId: row.versionId,

        textbookId: row.textbookId,

        chapterPath: [],

        chapterId: row.chapterId,

        chapterText: row.chapterText || ''

      }

      this.editOpen = true

    },

    onEditSubjectChange() {

      this.editCatalogForm = Object.assign(emptyCatalogForm(), { schoolStage: this.editCatalogForm.schoolStage || '\u9ad8\u4e2d' })

    },

    onEditStageChange() {

      this.editCatalogForm = Object.assign(emptyCatalogForm(), { schoolStage: this.editCatalogForm.schoolStage })

    },

    submitEdit() {

      this.editing = true

      updateLibraryDocument({

        ...this.editForm,

        schoolStage: this.editCatalogForm.schoolStage,

        versionId: this.editCatalogForm.versionId,

        textbookId: this.editCatalogForm.textbookId,

        chapterId: this.editCatalogForm.chapterId,

        chapterText: this.editCatalogForm.chapterText

      }).then(() => {

        this.$modal.msgSuccess('\u4fdd\u5b58\u6210\u529f')

        this.editOpen = false

        this.getList()

      }).finally(() => { this.editing = false })

    }

  }

}

</script>



<style scoped lang="scss">

.library-admin {

  .admin-tabs {

    margin-bottom: 12px;

  }



  .form-hint {

    margin: 0 0 8px 96px;

    font-size: 12px;

    color: #909399;

  }

}



.doc-table {

  ::v-deep .el-table__body td {
    padding: 12px 0;
    font-size: 13px;
    color: #334155;
  }

  ::v-deep .el-table__header th {
    font-size: 13px;
  }
}



.doc-cell-title {

  min-width: 0;

  &__main {
    display: flex;
    align-items: center;
    gap: 8px;
    min-width: 0;
  }

  &__meta {
    margin-top: 6px;
    font-size: 12px;
    color: #94a3b8;
    line-height: 1.4;
  }
}



.doc-ext-tag {
  flex-shrink: 0;
  padding: 2px 6px;
  border-radius: 4px;
  background: #f1f5f9;
  border: 1px solid #e2e8f0;
  font-size: 11px;
  font-weight: 600;
  color: #64748b;
  line-height: 1.4;
}



.doc-title-text {
  min-width: 0;
  flex: 1;
  font-size: 14px;
  font-weight: 600;
  color: #1e293b;
  line-height: 1.45;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}



.doc-recommend-icon {
  flex-shrink: 0;
  font-size: 14px;
  color: #f59e0b;
}



.meta-dot {
  margin: 0 2px;
}

.doc-price-tag {
  color: #ea580c;
  font-weight: 600;
}



.doc-cell-catalog {
  display: flex;
  flex-direction: column;
  gap: 6px;
  font-size: 13px;
  color: #475569;
  line-height: 1.45;
}



.doc-vis-tag {
  align-self: flex-start;
  padding: 1px 8px;
  border-radius: 999px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  font-size: 12px;
  color: #64748b;
}



.doc-status-group {
  display: inline-flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}



.doc-stats {
  display: flex;
  flex-direction: column;
  gap: 4px;
  font-size: 13px;
  color: #475569;

  span {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    gap: 4px;
  }

  i {
    font-size: 13px;
    color: #94a3b8;
  }
}



.doc-actions {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-wrap: wrap;
  gap: 2px;

  .el-button--text {
    padding: 4px 6px;
    font-size: 13px;
  }
}



.detail-drawer {

  padding: 0 8px 24px;



  dl {

    margin: 0;

  }



  dt {

    margin-top: 14px;

    font-size: 12px;

    color: #909399;

  }



  dd {

    margin: 4px 0 0;

    font-size: 13px;

    color: #303133;

    word-break: break-all;

  }



  .mono {

    font-family: Consolas, monospace;

    font-size: 12px;

  }



  .text-danger {

    color: #f56c6c;

  }

}



.health-path {

  margin-left: 8px;

  font-size: 12px;

  color: #606266;

  word-break: break-all;

}



.health-stats {

  margin-top: 4px;



  .stat-card {

    margin-bottom: 12px;

    padding: 10px 8px;

    text-align: center;

    background: #f5f7fa;

    border-radius: 6px;

  }



  .stat-num {

    font-size: 20px;

    font-weight: 600;

    color: #303133;

  }



  .stat-label {

    margin-top: 4px;

    font-size: 12px;

    color: #909399;

  }

}

.preview-settings-form {
  margin-top: 4px;
}

.preview-settings-hint {
  margin: 8px 0 0;
  font-size: 12px;
  color: #94a3b8;
  line-height: 1.5;
}

</style>


