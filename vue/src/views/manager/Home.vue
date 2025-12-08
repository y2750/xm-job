<template>
  <div>
    <div style="display: flex; margin-bottom: 20px">
      <a-card style="flex: 1; margin-right: 10px">
        <a-card-meta>
          <template #title>项目总数</template>
          <template #description>
            <div style="font-size: 32px; text-align: center; margin-top: 10px; color: #1890ff">{{ data.projectNum }}</div>
          </template>
        </a-card-meta>
      </a-card>
      <a-card style="flex: 1; margin: 0 10px">
        <a-card-meta>
          <template #title>稿件总数</template>
          <template #description>
            <div style="font-size: 32px; text-align: center; margin-top: 10px; color: #52c41a">{{ data.submissionNum }}</div>
          </template>
        </a-card-meta>
      </a-card>
      <a-card style="flex: 1; margin: 0 10px">
        <a-card-meta>
          <template #title>自由职业者</template>
          <template #description>
            <div style="font-size: 32px; text-align: center; margin-top: 10px; color: #faad14">{{ data.freelancerNum }}</div>
          </template>
        </a-card-meta>
      </a-card>
      <a-card style="flex: 1; margin-left: 10px">
        <a-card-meta>
          <template #title>企业用户</template>
          <template #description>
            <div style="font-size: 32px; text-align: center; margin-top: 10px; color: #f5222d">{{ data.enterpriseNum }}</div>
          </template>
        </a-card-meta>
      </a-card>
    </div>
    <a-card style="margin-top: 10px; height: 350px" title="数据统计图表">
      <div id="bar" style="height: 300px"></div>
    </a-card>
    <a-card style="margin-top: 10px; height: 350px" title="数据分布图表">
      <div id="pie" style="height: 300px"></div>
    </a-card>
  </div>
</template>

<script setup>
import { reactive, onMounted } from "vue";
import request from "@/utils/request.js";
import { message } from "ant-design-vue";
import * as echarts from "echarts";

const data = reactive({
  user: JSON.parse(localStorage.getItem('xm-user') || '{}'),
  projectNum: 0,
  submissionNum: 0,
  freelancerNum: 0,
  enterpriseNum: 0
})

const loadStatistics = async () => {
  try {
    // 加载项目数量
    const projectRes = await request.get('/api/projects', { params: { pageNum: 1, pageSize: 1 } })
    if (projectRes.code === '200') {
      data.projectNum = projectRes.data?.total || 0
    }
    
    // 加载稿件数量
    const submissionRes = await request.get('/api/submissions', { params: { pageNum: 1, pageSize: 1 } })
    if (submissionRes.code === '200') {
      data.submissionNum = submissionRes.data?.total || 0
    }
    
    // 加载自由职业者数量
    const freelancerRes = await request.get('/api/freelancers', { params: { pageNum: 1, pageSize: 1 } })
    if (freelancerRes.code === '200') {
      data.freelancerNum = freelancerRes.data?.total || 0
    }
    
    // 加载企业数量
    const enterpriseRes = await request.get('/api/enterprises', { params: { pageNum: 1, pageSize: 1 } })
    if (enterpriseRes.code === '200') {
      data.enterpriseNum = enterpriseRes.data?.total || 0
    }
  } catch (error) {
    console.error('加载统计信息失败:', error)
  }
}

const initCharts = () => {
  // 柱状图
  const barChart = echarts.init(document.getElementById('bar'));
  barChart.setOption({
    title: { text: '数据统计' },
    tooltip: {},
    xAxis: { data: ['项目', '稿件', '自由职业者', '企业'] },
    yAxis: {},
    series: [{
      name: '数量',
      type: 'bar',
      data: [data.projectNum, data.submissionNum, data.freelancerNum, data.enterpriseNum],
      itemStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: '#83bff6' },
          { offset: 0.5, color: '#188df0' },
          { offset: 1, color: '#188df0' }
        ])
      }
    }]
  });

  // 饼图
  const pieChart = echarts.init(document.getElementById('pie'));
  pieChart.setOption({
    title: { text: '数据分布' },
    tooltip: { trigger: 'item' },
    series: [{
      name: '数量',
      type: 'pie',
      radius: '50%',
      data: [
        { value: data.projectNum, name: '项目' },
        { value: data.submissionNum, name: '稿件' },
        { value: data.freelancerNum, name: '自由职业者' },
        { value: data.enterpriseNum, name: '企业' }
      ]
    }]
  });
}

onMounted(() => {
  loadStatistics()
  setTimeout(() => {
    initCharts()
  }, 500)
})
</script>

<style scoped>
.card {
  background-color: white;
  padding: 20px;
  border-radius: 5px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}
</style>
