# -*- coding: utf-8 -*-
import os

ROOT = os.path.join(os.path.dirname(__file__), '..', 'src')

FILES = {}

FILES['layout-portal/components/PortalHeader.vue'] = r'''<template>
  <div class="portal-header">
    <div class="portal-topbar">
      <div class="portal-container topbar-inner">
        <div class="topbar-left">
          <router-link to="/portal/home" class="portal-logo">
            <span class="logo-icon">��</span>
            <span class="logo-text">�ǻ����</span>
          </router-link>
        </div>
        <div class="topbar-search">
          <el-input
            v-model="keyword"
            placeholder="��������ɡ�֪ʶ����Ծ�����"
            clearable
            @keyup.enter.native="onSearch"
          >
            <el-button slot="append" type="primary" icon="el-icon-search" @click="onSearch">����</el-button>
          </el-input>
          <div v-if="hotWords.length" class="hot-words">
            ���ѣ�
            <span
              v-for="w in hotWords"
              :key="w"
              class="hot-word"
              @click="searchWord(w)"
            >{{ w }}</span>
          </div>
        </div>
        <div class="topbar-right">
          <template v-if="token">
            <span class="user-name">{{ nickName || name }}</span>
            <el-button type="text" @click="goAdmin">������̨</el-button>
            <el-button type="text" @click="logout">�˳�</el-button>
          </template>
          <template v-else>
            <el-button type="text" @click="goLogin">��¼</el-button>
            <el-button type="primary" size="small" @click="goLogin">ע��</el-button>
          </template>
        </div>
      </div>
    </div>
    <portal-nav :subject-id.sync="subjectId" :school-stage.sync="schoolStage" />
  </div>
</template>

<script>
import { mapGetters } from 'vuex'
import PortalNav from './PortalNav'

export default {
  name: 'PortalHeader',
  components: { PortalNav },
  data() {
    return {
      keyword: '',
      hotWords: ['ţ�ٵ�һ����', '�ȱ���ֱ���˶�', '��������', '�����غ�'],
      subjectId: null,
      schoolStage: '����'
    }
  },
  computed: {
    ...mapGetters(['token', 'name', 'nickName'])
  },
  methods: {
    onSearch() {
      const kw = (this.keyword || '').trim()
      this.$router.push({ path: '/portal/chapter', query: kw ? { keyword: kw } : {} })
    },
    searchWord(w) {
      this.keyword = w
      this.onSearch()
    },
    goLogin() {
      this.$router.push({ path: '/login', query: { redirect: this.$route.fullPath } })
    },
    goAdmin() {
      this.$router.push('/')
    },
    logout() {
      this.$store.dispatch('LogOut').then(() => {
        this.$router.push('/portal/home')
      })
    }
  }
}
</script>

<style scoped lang="scss">
.portal-topbar {
  background: #fff;
  border-bottom: 1px solid #e8edf3;
  padding: 14px 0 10px;
}

.topbar-inner {
  display: flex;
  align-items: flex-start;
  gap: 24px;
}

.portal-logo {
  display: flex;
  align-items: center;
  gap: 8px;
  text-decoration: none;
  color: #2877ff;
  font-weight: 700;
  font-size: 22px;
  white-space: nowrap;
}

.logo-icon {
  width: 40px;
  height: 40px;
  border-radius: 8px;
  background: linear-gradient(135deg, #2877ff, #5aa0ff);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
}

.topbar-search {
  flex: 1;
  min-width: 0;
}

.hot-words {
  margin-top: 6px;
  font-size: 12px;
  color: #909399;
}

.hot-word {
  margin-right: 12px;
  color: #f56c6c;
  cursor: pointer;
  &:hover { text-decoration: underline; }
}

.topbar-right {
  display: flex;
  align-items: center;
  gap: 8px;
  white-space: nowrap;
  padding-top: 4px;
}

.user-name {
  color: #606266;
  font-size: 13px;
  margin-right: 4px;
}
</style>
'''

# Write files
for rel, content in FILES.items():
    path = os.path.normpath(os.path.join(ROOT, rel))
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w', encoding='utf-8', newline='\n') as f:
        f.write(content)
    print('wrote', rel)

print('done')
