<template>
  <PageCard title="权限 ACL" subtitle="为 USER / ROLE 授予对本知识库的 READ、WRITE 或 ADMIN 权限。">
    <template #actions>
      <el-button round :loading="loading" @click="loadAcls">刷新</el-button>
    </template>

    <el-form label-position="top" size="default" class="acl-form" @submit.prevent="submitAcl">
      <div class="grid cols-2 compact-grid">
        <el-form-item label="主体类型">
          <el-select v-model="aclForm.principalType" class="full-width">
            <el-option label="USER" value="USER" />
            <el-option label="ROLE" value="ROLE" />
          </el-select>
        </el-form-item>
        <el-form-item label="主体 ID">
          <el-input v-model="aclForm.principalId" placeholder="alice / admin" />
        </el-form-item>
      </div>
      <el-form-item label="权限">
        <el-select v-model="aclForm.permission" class="full-width">
          <el-option label="READ" value="READ" />
          <el-option label="WRITE" value="WRITE" />
          <el-option label="ADMIN" value="ADMIN" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" round :loading="granting" native-type="submit">授予 ACL</el-button>
      </el-form-item>
    </el-form>

    <el-table v-if="acls.length" :data="acls" size="small" class="data-table" stripe>
      <el-table-column prop="principalType" label="主体" width="90" />
      <el-table-column prop="principalId" label="ID" min-width="120" />
      <el-table-column prop="permission" label="权限" width="100" />
      <el-table-column label="操作" width="90" fixed="right">
        <template #default="{ row }">
          <el-button link type="danger" @click="removeAcl(row.aclId)">撤销</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-empty v-else-if="!loading" description="暂无 ACL 记录" />
  </PageCard>
</template>

<script setup>
import { onMounted, ref } from 'vue';
import PageCard from '../../components/PageCard.vue';
import { useLibraryWorkspace } from '../../composables/libraryWorkspace';
import { grantAcl, listAcls, revokeAcl } from '../../api';
import { requestContext } from '../../context';

const { libraryId, showMessage } = useLibraryWorkspace();

const acls = ref([]);
const loading = ref(false);
const granting = ref(false);
const aclForm = ref({
  principalType: 'USER',
  principalId: '',
  permission: 'READ'
});

async function loadAcls() {
  loading.value = true;
  try {
    acls.value = await listAcls({
      tenantId: requestContext.tenantId,
      resourceType: 'LIBRARY',
      resourceId: libraryId.value
    });
  } catch (error) {
    showMessage(error.message, 'error');
  } finally {
    loading.value = false;
  }
}

async function submitAcl() {
  if (!aclForm.value.principalId) {
    showMessage('请填写主体 ID', 'error');
    return;
  }
  granting.value = true;
  try {
    await grantAcl({
      tenantId: requestContext.tenantId,
      resourceType: 'LIBRARY',
      resourceId: libraryId.value,
      principalType: aclForm.value.principalType,
      principalId: aclForm.value.principalId,
      permission: aclForm.value.permission
    });
    showMessage('ACL 已授予', 'success');
    aclForm.value.principalId = '';
    await loadAcls();
  } catch (error) {
    showMessage(error.message, 'error');
  } finally {
    granting.value = false;
  }
}

async function removeAcl(aclId) {
  try {
    await revokeAcl(aclId);
    showMessage('ACL 已撤销', 'success');
    await loadAcls();
  } catch (error) {
    showMessage(error.message, 'error');
  }
}

onMounted(loadAcls);
</script>

<style scoped>
.acl-form {
  max-width: 560px;
  margin-bottom: 20px;
}
</style>
