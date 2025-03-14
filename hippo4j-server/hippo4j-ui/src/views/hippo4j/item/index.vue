<template>
  <div class="app-container">
    <div class="filter-container">
      <el-input
        v-model="listQuery.itemId"
        clearable
        placeholder="项目"
        style="width: 200px"
        class="filter-item"
      />
      <el-button
        v-waves
        class="filter-item"
        type="primary"
        icon="el-icon-search"
        @click="fetchData"
      >
        搜索
      </el-button>
      <el-button
        class="filter-item"
        style="margin-left: 10px"
        type="primary"
        icon="el-icon-edit"
        :disabled="isEditDisabled"
        @click="handleCreate"
      >
        添加
      </el-button>
    </div>
    <el-table
      v-loading="listLoading"
      border
      :data="list"
      style="width: 100%"
      highlight-current-row
      element-loading-text="Loading"
    >
      <el-table-column label="序号" width="95">
        <template slot-scope="scope">{{ scope.$index + 1 }}</template>
      </el-table-column>
      <el-table-column label="租户">
        <template slot-scope="scope">{{ scope.row.tenantId }}</template>
      </el-table-column>
      <el-table-column label="项目">
        <template slot-scope="scope">{{ scope.row.itemId }}</template>
      </el-table-column>
      <el-table-column label="项目名称">
        <template slot-scope="scope">{{ scope.row.itemName }}</template>
      </el-table-column>
      <!-- <el-table-column label="项目简介">
        <template slot-scope="scope">{{ scope.row.itemDesc | ellipsis }}</template>
      </el-table-column> -->
      <el-table-column label="负责人">
        <template slot-scope="scope">{{ scope.row.owner }} </template>
      </el-table-column>
      <!-- <el-table-column label="创建时间">
        <template slot-scope="scope">{{ scope.row.gmtCreate }}</template>
      </el-table-column> -->
      <el-table-column label="修改时间">
        <template slot-scope="scope">{{ scope.row.gmtModified }}</template>
      </el-table-column>
      <el-table-column label="操作" width="90" class-name="small-padding fixed-width">
        <template slot-scope="{ row }">
          <el-button type="text" :disabled="isEditDisabled" size="small" @click="handleUpdate(row)">
            编辑
          </el-button>
          <el-button
            v-if="row.status !== 'deleted'"
            :disabled="isEditDisabled"
            size="small"
            type="text"
            @click="handleDelete(row)"
          >
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination
      v-show="total > 0"
      :total="total"
      :page.sync="listQuery.current"
      :limit.sync="listQuery.size"
      @pagination="fetchData"
    />

    <el-dialog :title="textMap[dialogStatus]" :visible.sync="dialogFormVisible" width="800px">
      <el-form
        ref="dataForm"
        :rules="rules"
        :model="temp"
        label-position="left"
        label-width="100px"
      >
        <el-form-item label="租户" prop="tenantId">
          <el-select
            v-model="temp.tenantId"
            placeholder="租户"
            filterable
            clearable
            class="filter-item"
            style="width: 40%"
            :disabled="dialogStatus === 'create' ? false : true"
          >
            <el-option
              v-for="item in tenantOptions"
              :key="item.key"
              :label="item.display_name"
              :value="item.key"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="项目" prop="itemId">
          <el-input
            v-model="temp.itemId"
            :disabled="dialogStatus === 'create' ? false : true"
            placeholder="项目"
            style="width: 40%"
          />
        </el-form-item>
        <el-form-item label="项目名称" prop="itemName">
          <el-input v-model="temp.itemName" placeholder="项目名称" style="width: 40%" />
        </el-form-item>
        <el-form-item label="负责人" prop="owner">
          <el-input v-model="temp.owner" placeholder="负责人" style="width: 40%" />
        </el-form-item>
        <el-form-item label="项目简介" prop="itemDesc">
          <el-input
            v-model="temp.itemDesc"
            :autosize="{ minRows: 3, maxRows: 6 }"
            type="textarea"
            placeholder="项目简介"
            style="width: 40%"
          />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="dialogFormVisible = false"> 取消 </el-button>
        <el-button type="primary" @click="dialogStatus === 'create' ? createData() : updateData()">
          确认
        </el-button>
      </div>
    </el-dialog>
    <el-dialog :visible.sync="dialogPluginVisible" title="Reading statistics">
      <el-table :data="pluginData" border fit highlight-current-row style="width: 100%">
        <el-table-column prop="key" label="Channel" />
        <el-table-column prop="pv" label="Pv" />
      </el-table>
      <span slot="footer" class="dialog-footer">
        <el-button type="primary" @click="dialogPvVisible = false">Confirm</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import * as jobProjectApi from '@/api/hippo4j-item';
import * as tenantApi from '@/api/hippo4j-tenant';
import waves from '@/directive/waves';
import Pagination from '@/components/Pagination';

export default {
  name: 'JobProject',
  components: { Pagination },
  directives: { waves },
  filters: {
    statusFilter(status) {
      const statusMap = {
        published: 'success',
        draft: 'gray',
        deleted: 'danger',
      };
      return statusMap[status];
    },
    ellipsis(value) {
      if (!value) return '';
      if (value.length > 26) {
        return value.slice(0, 26) + '...';
      }
      return value;
    },
  },
  data() {
    return {
      list: null,
      listLoading: true,
      total: 0,
      listQuery: {
        current: 1,
        size: 10,
        itemId: '',
        desc: true,
      },
      pluginTypeOptions: ['reader', 'writer'],
      dialogPluginVisible: false,
      pluginData: [],
      dialogFormVisible: false,
      tenantOptions: [],
      dialogStatus: '',
      isEditDisabled: false,
      textMap: {
        update: 'Edit',
        create: 'Create',
      },
      rules: {
        tenantId: [{ required: true, message: 'this is required', trigger: 'blur' }],
        itemId: [{ required: true, message: 'this is required', trigger: 'blur' }],
        itemName: [{ required: true, message: 'this is required', trigger: 'blur' }],
        owner: [{ required: true, message: 'this is required', trigger: 'blur' }],
        itemDesc: [{ required: true, message: 'this is required', trigger: 'blur' }],
      },
      temp: {
        id: undefined,
        tenantId: '',
        tenantName: '',
        owner: '',
        tenantDesc: '',
      },
      visible: true,
    };
  },
  created() {
    this.fetchData();
    // 初始化租户
    this.initSelect();
  },
  mounted() {
    this.isEditDisabled = localStorage.getItem('USER_ROLE') !== 'ROLE_ADMIN';
  },
  methods: {
    fetchData() {
      this.listLoading = true;
      jobProjectApi.list(this.listQuery).then((response) => {
        const { records } = response;
        const { total } = response;
        this.total = total;
        this.list = records;
        this.listLoading = false;
      });
    },
    initSelect() {
      tenantApi.list({}).then((response) => {
        const { records } = response;
        for (var i = 0; i < records.length; i++) {
          this.tenantOptions.push({
            key: records[i].tenantId,
            display_name: records[i].tenantId + ' ' + records[i].tenantName,
          });
        }
      });
    },
    resetTemp() {
      this.temp = {
        id: undefined,
        tenantName: '',
        tenantDesc: '',
      };
    },
    handleCreate() {
      this.resetTemp();
      this.dialogStatus = 'create';
      this.dialogFormVisible = true;
      this.$nextTick(() => {
        this.$refs['dataForm'].clearValidate();
      });
    },
    createData() {
      this.$refs['dataForm'].validate((valid) => {
        if (valid) {
          jobProjectApi.created(this.temp).then(() => {
            this.fetchData();
            this.dialogFormVisible = false;
            this.$notify({
              title: 'Success',
              message: 'Created Successfully',
              type: 'success',
              duration: 2000,
            });
          });
        }
      });
    },
    handleUpdate(row) {
      this.temp = Object.assign({}, row); // copy obj
      this.dialogStatus = 'update';
      this.dialogFormVisible = true;
      this.$nextTick(() => {
        this.$refs['dataForm'].clearValidate();
      });
    },
    updateData() {
      this.$refs['dataForm'].validate((valid) => {
        if (valid) {
          const tempData = Object.assign({}, this.temp);
          jobProjectApi.updated(tempData).then(() => {
            this.fetchData();
            this.dialogFormVisible = false;
            this.$notify({
              title: 'Success',
              message: 'Update Successfully',
              type: 'success',
              duration: 2000,
            });
          });
        }
      });
    },
    openDelConfirm(name) {
      return this.$confirm(`此操作将删除 ${name}, 是否继续?`, '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      });
    },
    handleDelete(row) {
      this.openDelConfirm(row.itemId).then(() => {
        const delObj = [row.tenantId, row.itemId];
        jobProjectApi.deleted(delObj).then((response) => {
          this.fetchData();
          this.$notify({
            title: 'Success',
            message: 'Delete Successfully',
            type: 'success',
            duration: 2000,
          });
        });
      });
    },
  },
};
</script>
