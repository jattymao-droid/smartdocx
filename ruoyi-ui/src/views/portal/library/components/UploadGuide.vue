<template>
  <aside class="upload-guide portal-card">
    <div class="upload-guide-head">
      <i class="el-icon-info" />
      <h3>{{ title }}</h3>
    </div>

    <section v-for="block in blocks" :key="block.title" class="guide-block">
      <h4>
        <i :class="block.icon" />
        {{ block.title }}
      </h4>
      <ul>
        <li v-for="(item, idx) in block.items" :key="idx">{{ item }}</li>
      </ul>
    </section>

    <div class="guide-tip">
      <i class="el-icon-warning-outline" />
      <p>{{ tip }}</p>
    </div>
  </aside>
</template>

<script>
export default {
  name: 'UploadGuide',
  props: {
    title: {
      type: String,
      default: '\u4e0a\u4f20\u8bf4\u660e'
    },
    tip: {
      type: String,
      default: '\u8bf7\u786e\u4fdd\u4e0a\u4f20\u5185\u5bb9\u62e5\u6709\u5408\u6cd5\u4f7f\u7528\u6743\uff0c\u4e0d\u5f97\u4e0a\u4f20\u4fb5\u6743\u3001\u6d89\u5bc6\u6216\u8fdd\u89c4\u8d44\u6599\u3002'
    }
  },
  data() {
    return {
      blocks: [
        {
          title: '\u652f\u6301\u683c\u5f0f',
          icon: 'el-icon-document',
          items: [
            '\u6587\u6863\uff1aPDF\u3001Word\uff08doc/docx\uff09\u3001PPT\uff08ppt/pptx\uff09',
            '\u8868\u683c\uff1aExcel\uff08xls/xlsx\uff09\u3001TXT \u6587\u672c',
            '\u538b\u7f29\u5305\uff1aZIP\u3001RAR\u30017Z\uff08\u53ef\u5728\u9884\u89c8\u9875\u5c55\u5f00\u67e5\u770b\u5185\u90e8\u6587\u6863\uff09',
            '\u5355\u6587\u4ef6\u5927\u5c0f\u4e0d\u8d85\u8fc7 50MB'
          ]
        },
        {
          title: '\u586b\u5199\u5efa\u8bae',
          icon: 'el-icon-edit-outline',
          items: [
            '\u6807\u9898\u5e94\u7b80\u6d01\u660e\u786e\uff0c\u4fbf\u4e8e\u540c\u4e8b\u68c0\u7d22',
            '\u5efa\u8bae\u586b\u5199\u5b66\u6bb5\u3001\u5b66\u79d1\u3001\u7248\u672c\u3001\u6559\u6750\u4e0e\u7ae0\u8282\uff0c\u63d0\u5347\u68c0\u7d22\u51c6\u786e\u5ea6',
            '\u5efa\u8bae\u586b\u5199\u5206\u7c7b\u4e0e\u6807\u7b7e\uff0c\u63d0\u5347\u66dd\u5149',
            '\u5c01\u9762\u53ef\u9009\uff0c\u672a\u4e0a\u4f20\u5c06\u81ea\u52a8\u622a\u53d6\u6587\u6863\u9996\u9875',
            '\u591a\u4e2a\u6807\u7b7e\u8bf7\u7528\u82f1\u6587\u9017\u53f7\u5206\u9694'
          ]
        },
        {
          title: '\u53ef\u89c1\u8303\u56f4',
          icon: 'el-icon-view',
          items: [
            '\u516c\u5f00\uff1a\u6240\u6709\u767b\u5f55\u7528\u6237\u53ef\u89c1',
            '\u6821\u5185\uff1a\u672c\u6821\u6559\u5e08\u53ef\u89c1\uff08\u63a8\u8350\uff09',
            '\u4ec5\u672c\u4eba\uff1a\u4ec5\u81ea\u5df1\u53ef\u89c1\uff0c\u9002\u5408\u8349\u7a3f'
          ]
        },
        {
          title: '\u5ba1\u6838\u4e0e\u53d1\u5e03',
          icon: 'el-icon-circle-check',
          items: [
            '\u63d0\u4ea4\u540e\u53ef\u80fd\u8fdb\u5165\u7ba1\u7406\u5458\u5ba1\u6838\u6d41\u7a0b',
            '\u5ba1\u6838\u901a\u8fc7\u540e\u65b9\u53ef\u5728\u6587\u5e93\u516c\u5f00\u5c55\u793a',
            '\u53ef\u5728\u300c\u6211\u7684\u4e0a\u4f20\u300d\u4e2d\u67e5\u770b\u5ba1\u6838\u72b6\u6001',
            '\u5ba1\u6838\u671f\u95f4\u8bf7\u52ff\u91cd\u590d\u4e0a\u4f20\u76f8\u540c\u6587\u4ef6'
          ]
        }
      ]
    }
  }
}
</script>

<style scoped lang="scss">
.upload-guide {
  padding: 18px 18px 16px;
  position: sticky;
  top: 16px;
}

.upload-guide-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 14px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f1f5f9;

  i {
    width: 28px;
    height: 28px;
    border-radius: 8px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 15px;
    color: #0f766e;
    background: linear-gradient(135deg, #ecfeff, #ccfbf1);
  }

  h3 {
    margin: 0;
    font-size: 15px;
    font-weight: 700;
    color: #0f172a;
  }
}

.guide-block {
  & + & {
    margin-top: 14px;
  }

  h4 {
    display: flex;
    align-items: center;
    gap: 6px;
    margin: 0 0 8px;
    font-size: 13px;
    font-weight: 700;
    color: #334155;

    i {
      font-size: 14px;
      color: #0f766e;
    }
  }

  ul {
    margin: 0;
    padding: 0 0 0 18px;
    list-style: disc;
  }

  li {
    font-size: 12px;
    color: #64748b;
    line-height: 1.65;
    margin-bottom: 4px;

    &:last-child {
      margin-bottom: 0;
    }
  }
}

.guide-tip {
  display: flex;
  gap: 8px;
  margin-top: 16px;
  padding: 10px 12px;
  border-radius: 10px;
  background: #fffbeb;
  border: 1px solid #fde68a;

  i {
    flex-shrink: 0;
    margin-top: 2px;
    font-size: 15px;
    color: #d97706;
  }

  p {
    margin: 0;
    font-size: 12px;
    color: #92400e;
    line-height: 1.6;
  }
}
</style>
