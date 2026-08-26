<template>
  <div class="song-management">
    <!-- 操作栏 -->
    <div class="toolbar">
      <el-button
        type="primary"
        icon="Plus"
        @click="showAddDialog"
      >
        新增歌曲
      </el-button>
      <el-button
        type="success"
        icon="Edit"
        @click="showBatchEditDialog"
      >
        批量编辑类型和语言
      </el-button>
      <el-button
        icon="Refresh"
        @click="fetchSongs"
      >
        刷新
      </el-button>
    </div>

    <!-- 歌曲列表 -->
    <el-table
      v-loading="loading"
      :data="songList"
      border
      stripe
      style="width: 100%"
    >
      <el-table-column
        type="index"
        label="序号"
        width="60"
        align="center"
      />

      <el-table-column
        label="封面"
        width="80"
        align="center"
      >
        <template #default="{ row }">
          <el-image
            style="width: 50px; height: 50px; border-radius: 4px;"
            :src="row.cover"
            fit="cover"
          >
            <template #error>
              <div class="image-slot">
                🎵
              </div>
            </template>
          </el-image>
        </template>
      </el-table-column>

      <el-table-column
        prop="title"
        label="歌曲名称"
        width="200"
      />

      <el-table-column
        label="歌手"
        width="150"
      >
        <template #default="{ row }">
          {{ row.artists && row.artists.length > 0 ? row.artists.map(a => a.name).join(' / ') : '-' }}
        </template>
      </el-table-column>

      <el-table-column
        prop="albumName"
        label="专辑"
        width="150"
        show-overflow-tooltip
      />
            
      <el-table-column
        prop="genre"
        label="类型"
        width="100"
        align="center"
      >
        <template #default="{ row }">
          <el-tag
            v-if="row.genre"
            size="small"
            type="info"
          >
            {{ row.genre }}
          </el-tag>
          <span
            v-else
            style="color: #999;"
          >未设置</span>
        </template>
      </el-table-column>
            
      <el-table-column
        prop="language"
        label="语言"
        width="100"
        align="center"
      >
        <template #default="{ row }">
          <el-tag
            v-if="row.language"
            size="small"
            type="success"
          >
            {{ row.language }}
          </el-tag>
          <span
            v-else
            style="color: #999;"
          >未设置</span>
        </template>
      </el-table-column>

      <el-table-column
        prop="duration"
        label="时长"
        width="80"
        align="center"
      >
        <template #default="{ row }">
          {{ formatDuration(row.duration) }}
        </template>
      </el-table-column>

      <el-table-column
        prop="playCount"
        label="播放次数"
        width="100"
        align="center"
      />

      <el-table-column
        prop="status"
        label="状态"
        width="80"
        align="center"
      >
        <template #default="{ row }">
          <el-tag
            :type="row.status === 1 ? 'success' : 'danger'"
            size="small"
          >
            {{ row.status === 1 ? '上架' : '下架' }}
          </el-tag>
        </template>
      </el-table-column>

      <el-table-column
        prop="createTime"
        label="创建时间"
        width="180"
      >
        <template #default="{ row }">
          {{ formatDate(row.createTime) }}
        </template>
      </el-table-column>

      <el-table-column
        label="操作"
        width="180"
        fixed="right"
        align="center"
      >
        <template #default="{ row }">
          <el-button
            type="primary"
            size="small"
            @click="showEditDialog(row)"
          >
            编辑
          </el-button>
          <el-popconfirm
            title="确定要删除这首歌曲吗？"
            @confirm="handleDelete(row.id)"
          >
            <template #reference>
              <el-button
                type="danger"
                size="small"
              >
                删除
              </el-button>
            </template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <el-pagination
      v-model:current-page="pagination.page"
      v-model:page-size="pagination.size"
      :total="pagination.total"
      :page-sizes="[10, 20, 50, 100]"
      layout="total, sizes, prev, pager, next, jumper"
      style="margin-top: 20px; justify-content: center;"
      @size-change="fetchSongs"
      @current-change="fetchSongs"
    />

    <!-- 新增/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogMode === 'add' ? '新增歌曲' : '编辑歌曲'"
      width="700px"
      :close-on-click-modal="false"
    >
      <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-width="100px"
      >
        <el-form-item
          label="歌曲名称"
          prop="title"
        >
          <el-input
            v-model="formData.title"
            placeholder="请输入歌曲名称"
            maxlength="100"
            show-word-limit
          />
        </el-form-item>

        <el-form-item
          label="歌手"
          prop="artistIds"
        >
          <el-select
            v-model="formData.artistIds"
            placeholder="请选择歌手（可多选）"
            filterable
            multiple 
            collapse-tags
            collapse-tags-tooltip
            style="width: 100%"
          >
            <el-option
              v-for="artist in artists"
              :key="artist.id"
              :label="artist.name"
              :value="artist.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item
          label="专辑名称"
          prop="albumName"
        >
          <el-input
            v-model="formData.albumName"
            placeholder="请输入专辑名称（可选）"
            maxlength="100"
          />
        </el-form-item>

        <el-form-item
          label="时长（秒）"
          prop="duration"
        >
          <el-input-number
            v-model="formData.duration"
            :min="0"
            :max="7200"
            placeholder="例如：245"
            style="width: 100%"
          />
          <span style="margin-left: 10px; color: #999;">
            {{ formatDuration(formData.duration) }}
          </span>
        </el-form-item>

        <el-form-item
          label="封面图片"
          prop="cover"
        >
          <el-upload
            class="cover-uploader"
            :action="uploadCoverAction"
            :headers="uploadHeaders"
            :show-file-list="false"
            :on-success="handleCoverSuccess"
            :before-upload="beforeCoverUpload"
            :on-progress="() => coverUploading = true"
          >
            <img
              v-if="formData.cover"
              :src="formData.cover"
              class="cover-image"
            >
            <el-icon
              v-else
              class="cover-uploader-icon"
              :class="{ uploading: coverUploading }"
            >
              <Plus />
            </el-icon>
          </el-upload>
          <div style="color: #999; font-size: 12px; margin-top: 5px;">
            点击上传封面图片，支持 JPG/PNG，大小不超过 2MB
          </div>
        </el-form-item>

        <el-form-item
          label="音乐文件"
          prop="url"
        >
          <el-upload
            :action="uploadMusicAction"
            :headers="uploadHeaders"
            :on-success="handleMusicSuccess"
            :before-upload="beforeMusicUpload"
            :on-progress="() => musicUploading = true"
            :show-file-list="false"
          >
            <el-button
              type="primary"
              :loading="musicUploading"
            >
              {{ musicUploading ? '上传中...' : (formData.url ? '重新上传' : '上传音乐') }}
            </el-button>
          </el-upload>
          <div
            v-if="formData.url"
            style="margin-top: 10px;"
          >
            <el-tag type="success">
              ✓ 已上传
            </el-tag>
            <span style="margin-left: 10px; color: #999; font-size: 12px;">
              {{ formData.url.substring(formData.url.lastIndexOf('/') + 1) }}
            </span>
          </div>
          <div style="color: #999; font-size: 12px; margin-top: 5px;">
            支持 MP3/WAV/FLAC 等格式，大小不超过 30MB
          </div>
        </el-form-item>

        <el-form-item
          label="歌词"
          prop="lyric"
        >
          <el-input
            v-model="formData.lyric"
            type="textarea"
            :rows="6"
            placeholder="请输入歌词（LRC 格式），例如：&#10;[00:00.00]歌曲标题&#10;[00:05.00]歌手名&#10;[00:20.50]歌词第一句"
          />
          <div style="color: #999; font-size: 12px; margin-top: 5px;">
            可选，支持 LRC 格式歌词
          </div>
        </el-form-item>

        <el-form-item
          label="状态"
          prop="status"
        >
          <el-radio-group v-model="formData.status">
            <el-radio :label="1">
              上架
            </el-radio>
            <el-radio :label="0">
              下架
            </el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">
          取消
        </el-button>
        <el-button
          type="primary"
          :loading="submitLoading"
          @click="handleSubmit"
        >
          确定
        </el-button>
      </template>
    </el-dialog>

    <!-- 批量编辑类型和语言对话框 -->
    <el-dialog
      v-model="batchEditDialogVisible"
      title="批量编辑歌曲类型和语言"
      width="90%"
      top="5vh"
      :close-on-click-modal="false"
    >
      <div class="batch-edit-container">
        <div class="batch-edit-toolbar">
          <el-alert
            type="info"
            :closable="false"
            show-icon
          >
            <template #title>
              <span>共 {{ batchSongList.length }} 首歌曲，可直接在表格中编辑类型和语言</span>
            </template>
          </el-alert>
          <div style="margin-top: 10px;">
            <el-button
              type="primary"
              icon="Select"
              @click="quickSetGenre"
            >
              快速设置类型
            </el-button>
            <el-button
              type="success"
              icon="Select"
              @click="quickSetLanguage"
            >
              快速设置语言
            </el-button>
            <el-input
              v-model="batchSearchKeyword"
              placeholder="搜索歌曲名称或歌手"
              style="width: 300px; margin-left: 10px;"
              clearable
            >
              <template #prefix>
                <el-icon><Search /></el-icon>
              </template>
            </el-input>
          </div>
        </div>

        <el-table
          v-loading="batchLoading"
          :data="filteredBatchSongList"
          border
          stripe 
          style="width: 100%; margin-top: 15px;"
          max-height="500"
        >
          <el-table-column
            type="index"
            label="序号"
            width="60"
            align="center"
          />
                    
          <el-table-column
            label="封面"
            width="70"
            align="center"
          >
            <template #default="{ row }">
              <el-image
                style="width: 40px; height: 40px; border-radius: 4px;" 
                :src="row.cover"
                fit="cover"
              >
                <template #error>
                  <div class="image-slot-small">
                    🎵
                  </div>
                </template>
              </el-image>
            </template>
          </el-table-column>

          <el-table-column
            prop="title"
            label="歌曲名称"
            width="200"
            show-overflow-tooltip
          />
                    
          <el-table-column
            label="歌手"
            width="150"
            show-overflow-tooltip
          >
            <template #default="{ row }">
              {{ row.artists && row.artists.length > 0 ? row.artists.map(a => a.name).join(' / ') : '-' }}
            </template>
          </el-table-column>

          <el-table-column
            label="类型"
            width="150"
            align="center"
          >
            <template #default="{ row }">
              <el-select
                v-model="row.genre"
                size="small"
                placeholder="选择类型"
                clearable
              >
                <el-option
                  label="流行"
                  value="流行"
                />
                <el-option
                  label="摇滚"
                  value="摇滚"
                />
                <el-option
                  label="民谣"
                  value="民谣"
                />
                <el-option
                  label="电子"
                  value="电子"
                />
                <el-option
                  label="说唱"
                  value="说唱"
                />
                <el-option
                  label="R&B"
                  value="R&B"
                />
                <el-option
                  label="爵士"
                  value="爵士"
                />
                <el-option
                  label="古典"
                  value="古典"
                />
                <el-option
                  label="轻音乐"
                  value="轻音乐"
                />
                <el-option
                  label="纯音乐"
                  value="纯音乐"
                />
              </el-select>
            </template>
          </el-table-column>

          <el-table-column
            label="语言"
            width="150"
            align="center"
          >
            <template #default="{ row }">
              <el-select
                v-model="row.language"
                size="small"
                placeholder="选择语言"
                clearable
              >
                <el-option
                  label="国语"
                  value="国语"
                />
                <el-option
                  label="粤语"
                  value="粤语"
                />
                <el-option
                  label="英语"
                  value="英语"
                />
                <el-option
                  label="日语"
                  value="日语"
                />
                <el-option
                  label="韩语"
                  value="韩语"
                />
                <el-option
                  label="其他"
                  value="其他"
                />
              </el-select>
            </template>
          </el-table-column>

          <el-table-column
            label="当前值"
            width="200"
            align="center"
          >
            <template #default="{ row }">
              <div style="display: flex; flex-direction: column; gap: 5px;">
                <el-tag
                  v-if="row.genre"
                  size="small"
                  type="info"
                >
                  类型: {{ row.genre }}
                </el-tag>
                <el-tag
                  v-if="row.language"
                  size="small"
                  type="success"
                >
                  语言: {{ row.language }}
                </el-tag>
              </div>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <template #footer>
        <el-button @click="batchEditDialogVisible = false">
          取消
        </el-button>
        <el-button
          type="primary"
          :loading="batchSubmitLoading"
          @click="handleBatchSubmit"
        >
          保存修改
        </el-button>
      </template>
    </el-dialog>

    <!-- 快速设置类型对话框 -->
    <el-dialog
      v-model="quickGenreDialogVisible"
      title="快速设置类型"
      width="400px"
    >
      <el-form label-width="80px">
        <el-form-item label="选择类型">
          <el-select
            v-model="quickGenreValue"
            placeholder="请选择类型"
            style="width: 100%"
          >
            <el-option
              label="流行"
              value="流行"
            />
            <el-option
              label="摇滚"
              value="摇滚"
            />
            <el-option
              label="民谣"
              value="民谣"
            />
            <el-option
              label="电子"
              value="电子"
            />
            <el-option
              label="说唱"
              value="说唱"
            />
            <el-option
              label="R&B"
              value="R&B"
            />
            <el-option
              label="爵士"
              value="爵士"
            />
            <el-option
              label="古典"
              value="古典"
            />
            <el-option
              label="轻音乐"
              value="轻音乐"
            />
            <el-option
              label="纯音乐"
              value="纯音乐"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="quickGenreDialogVisible = false">
          取消
        </el-button>
        <el-button
          type="primary"
          @click="applyQuickGenre"
        >
          应用到所有歌曲
        </el-button>
      </template>
    </el-dialog>

    <!-- 快速设置语言对话框 -->
    <el-dialog
      v-model="quickLanguageDialogVisible"
      title="快速设置语言"
      width="400px"
    >
      <el-form label-width="80px">
        <el-form-item label="选择语言">
          <el-select
            v-model="quickLanguageValue"
            placeholder="请选择语言"
            style="width: 100%"
          >
            <el-option
              label="国语"
              value="国语"
            />
            <el-option
              label="粤语"
              value="粤语"
            />
            <el-option
              label="英语"
              value="英语"
            />
            <el-option
              label="日语"
              value="日语"
            />
            <el-option
              label="韩语"
              value="韩语"
            />
            <el-option
              label="其他"
              value="其他"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="quickLanguageDialogVisible = false">
          取消
        </el-button>
        <el-button
          type="primary"
          @click="applyQuickLanguage"
        >
          应用到所有歌曲
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, Search } from '@element-plus/icons-vue'
import { getSongList, createSong, updateSong, deleteSong, batchUpdateGenreAndLanguage } from '@/api/song'
import { getAllArtists } from '@/api/artist'
import request from '@/utils/request'

export default {
    name: 'SongManagement',
    components: {
        Plus,
        Search
    },
    setup() {
        const loading = ref(false)
        const submitLoading = ref(false)
        const coverUploading = ref(false)
        const musicUploading = ref(false)
        const songList = ref([])
        const artists = ref([])
        const dialogVisible = ref(false)
        const dialogMode = ref('add')
        const formRef = ref(null)
        
        // 批量编辑相关
        const batchEditDialogVisible = ref(false)
        const batchLoading = ref(false)
        const batchSubmitLoading = ref(false)
        const batchSongList = ref([])
        const batchSearchKeyword = ref('')
        const quickGenreDialogVisible = ref(false)
        const quickLanguageDialogVisible = ref(false)
        const quickGenreValue = ref('')
        const quickLanguageValue = ref('')

        // 上传配置
        const uploadCoverAction = '/api/upload/cover'
        const uploadMusicAction = '/api/upload/music'
        const uploadHeaders = computed(() => ({
            Authorization: `Bearer ${localStorage.getItem('token')}`
        }))

        // 分页
        const pagination = reactive({
            page: 1,
            size: 10,
            total: 0
        })

        // 表单数据
        const formData = reactive({
            id: null,
            title: '',
            artistIds: [],  // 改为数组，支持多歌手
            albumName: '',
            duration: null,
            cover: '',
            url: '',
            lyric: '',
            status: 1
        })

        // 表单验证规则
        const formRules = {
            title: [
                { required: true, message: '请输入歌曲名称', trigger: 'blur' }
            ],
            artistIds: [
                { required: true, type: 'array', min: 1, message: '请至少选择一位歌手', trigger: 'change' }
            ],
            url: [
                { required: true, message: '请上传音乐文件', trigger: 'change' }
            ]
        }

        // 获取歌曲列表
        const fetchSongs = async () => {
            loading.value = true
            try {
                const res = await getSongList({
                    page: pagination.page - 1,
                    size: pagination.size
                })
                songList.value = res.data.content || []
                pagination.total = res.data.totalElements || 0
            } catch (error) {
                ElMessage.error('获取歌曲列表失败')
            } finally {
                loading.value = false
            }
        }

        // 获取所有歌手
        const fetchArtists = async () => {
            try {
                const res = await getAllArtists()
                artists.value = res.data || []
            } catch (error) {
                ElMessage.error('获取歌手列表失败')
            }
        }

        // 显示新增对话框
        const showAddDialog = () => {
            dialogMode.value = 'add'
            resetForm()
            dialogVisible.value = true
        }

        // 显示编辑对话框
        const showEditDialog = (row) => {
            dialogMode.value = 'edit'
            formData.id = row.id
            formData.title = row.title
            // 提取歌手IDs（支持多歌手）
            formData.artistIds = row.artists && row.artists.length > 0 
                ? row.artists.map(a => a.id) 
                : []
            formData.albumName = row.albumName || ''
            formData.duration = row.duration
            formData.cover = row.cover || ''
            formData.url = row.url
            formData.lyric = row.lyric || ''
            formData.status = row.status
            dialogVisible.value = true
        }

        // 重置表单
        const resetForm = () => {
            formData.id = null
            formData.title = ''
            formData.artistIds = []  // 重置为空数组
            formData.albumName = ''
            formData.duration = null
            formData.cover = ''
            formData.url = ''
            formData.lyric = ''
            formData.status = 1
            coverUploading.value = false
            musicUploading.value = false
            if (formRef.value) {
                formRef.value.clearValidate()
            }
        }

        // 封面上传前验证
        const beforeCoverUpload = (file) => {
            const isImage = file.type.startsWith('image/')
            const isLt2M = file.size / 1024 / 1024 < 2

            if (!isImage) {
                ElMessage.error('只能上传图片文件!')
                return false
            }
            if (!isLt2M) {
                ElMessage.error('图片大小不能超过 2MB!')
                return false
            }
            return true
        }

        // 封面上传成功
        const handleCoverSuccess = (response) => {
            coverUploading.value = false
            // 响应格式：{ code: 200, message: "xxx", data: { url: "xxx" } }
            const url = response?.data?.url || response?.url
            if (url) {
                formData.cover = url
                ElMessage.success('封面上传成功')
            } else {
                ElMessage.error('封面上传失败')
            }
        }

        // 音乐上传前验证
        const beforeMusicUpload = (file) => {
            const isAudio = file.type.startsWith('audio/')
            const isLt30M = file.size / 1024 / 1024 < 30

            if (!isAudio) {
                ElMessage.error('只能上传音频文件!')
                return false
            }
            if (!isLt30M) {
                ElMessage.error('音频文件大小不能超过 30MB!')
                return false
            }
            return true
        }

        // 音乐上传成功
        const handleMusicSuccess = (response) => {
            musicUploading.value = false
            // 响应格式：{ code: 200, message: "xxx", data: { url: "xxx" } }
            const url = response?.data?.url || response?.url
            if (url) {
                formData.url = url
                ElMessage.success('音乐文件上传成功')
            } else {
                ElMessage.error('音乐文件上传失败')
            }
        }

        // 提交表单
        const handleSubmit = async () => {
            if (!formRef.value) return

            await formRef.value.validate(async (valid) => {
                if (!valid) return

                try {
                    submitLoading.value = true

                    const data = {
                        title: formData.title,
                        artistIds: formData.artistIds,  // 发送歌手ID数组
                        albumName: formData.albumName || null,
                        duration: formData.duration,
                        cover: formData.cover || null,
                        url: formData.url,
                        lyric: formData.lyric || null,
                        status: formData.status
                    }

                    if (dialogMode.value === 'add') {
                        await createSong(data)
                        ElMessage.success('新增成功')
                    } else {
                        await updateSong(formData.id, data)
                        ElMessage.success('更新成功')
                    }

                    dialogVisible.value = false
                    fetchSongs()
                } catch (error) {
                    ElMessage.error(error.message || '操作失败')
                } finally {
                    submitLoading.value = false
                }
            })
        }

        // 删除歌曲
        const handleDelete = async (id) => {
            try {
                await deleteSong(id)
                ElMessage.success('删除成功')
                fetchSongs()
            } catch (error) {
                ElMessage.error(error.message || '删除失败')
            }
        }

        // 格式化时长
        const formatDuration = (seconds) => {
            if (!seconds) return '00:00'
            const min = Math.floor(seconds / 60)
            const sec = seconds % 60
            return `${String(min).padStart(2, '0')}:${String(sec).padStart(2, '0')}`
        }

        // 格式化日期
        const formatDate = (dateString) => {
            if (!dateString) return '-'
            const date = new Date(dateString)
            return date.toLocaleString('zh-CN')
        }

        // 显示批量编辑对话框
        const showBatchEditDialog = async () => {
            batchLoading.value = true
            batchEditDialogVisible.value = true
            try {
                // 获取所有歌曲（不分页）
                const res = await getSongList({
                    page: 0,
                    size: 9999 // 获取所有歌曲
                })
                batchSongList.value = (res.data.content || []).map(song => ({
                    ...song,
                    genre: song.genre || '',
                    language: song.language || ''
                }))
            } catch (error) {
                ElMessage.error('获取歌曲列表失败')
            } finally {
                batchLoading.value = false
            }
        }

        // 过滤后的批量编辑歌曲列表
        const filteredBatchSongList = computed(() => {
            if (!batchSearchKeyword.value) {
                return batchSongList.value
            }
            const keyword = batchSearchKeyword.value.toLowerCase()
            return batchSongList.value.filter(song => {
                const title = song.title?.toLowerCase() || ''
                const artistNames = song.artists?.map(a => a.name.toLowerCase()).join(' ') || ''
                return title.includes(keyword) || artistNames.includes(keyword)
            })
        })

        // 快速设置类型
        const quickSetGenre = () => {
            quickGenreValue.value = ''
            quickGenreDialogVisible.value = true
        }

        // 应用快速设置类型
        const applyQuickGenre = () => {
            if (!quickGenreValue.value) {
                ElMessage.warning('请选择类型')
                return
            }
            batchSongList.value.forEach(song => {
                song.genre = quickGenreValue.value
            })
            ElMessage.success('已应用到所有歌曲')
            quickGenreDialogVisible.value = false
        }

        // 快速设置语言
        const quickSetLanguage = () => {
            quickLanguageValue.value = ''
            quickLanguageDialogVisible.value = true
        }

        // 应用快速设置语言
        const applyQuickLanguage = () => {
            if (!quickLanguageValue.value) {
                ElMessage.warning('请选择语言')
                return
            }
            batchSongList.value.forEach(song => {
                song.language = quickLanguageValue.value
            })
            ElMessage.success('已应用到所有歌曲')
            quickLanguageDialogVisible.value = false
        }

        // 提交批量编辑
        const handleBatchSubmit = async () => {
            batchSubmitLoading.value = true
            try {
                // 准备更新数据
                const updates = batchSongList.value.map(song => ({
                    id: song.id,
                    genre: song.genre || '',
                    language: song.language || ''
                }))

                await batchUpdateGenreAndLanguage(updates)
                ElMessage.success('批量更新成功')
                batchEditDialogVisible.value = false
                // 刷新主列表
                fetchSongs()
            } catch (error) {
                ElMessage.error('批量更新失败：' + (error.message || '未知错误'))
            } finally {
                batchSubmitLoading.value = false
            }
        }

        // 初始化
        onMounted(() => {
            fetchSongs()
            fetchArtists()
        })

        return {
            loading,
            submitLoading,
            coverUploading,
            musicUploading,
            songList,
            artists,
            pagination,
            dialogVisible,
            dialogMode,
            formRef,
            formData,
            formRules,
            uploadCoverAction,
            uploadMusicAction,
            uploadHeaders,
            fetchSongs,
            showAddDialog,
            showEditDialog,
            beforeCoverUpload,
            handleCoverSuccess,
            beforeMusicUpload,
            handleMusicSuccess,
            handleSubmit,
            handleDelete,
            formatDuration,
            formatDate,
            // 批量编辑相关
            batchEditDialogVisible,
            batchSongList,
            batchLoading,
            batchSubmitLoading,
            batchSearchKeyword,
            filteredBatchSongList,
            quickGenreDialogVisible,
            quickLanguageDialogVisible,
            quickGenreValue,
            quickLanguageValue,
            showBatchEditDialog,
            quickSetGenre,
            quickSetLanguage,
            applyQuickGenre,
            applyQuickLanguage,
            handleBatchSubmit
        }
    }
}
</script>

<style scoped>
.song-management {
    padding: 20px;
}

.toolbar {
    margin-bottom: 20px;
    display: flex;
    gap: 10px;
}

.image-slot {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 100%;
    height: 100%;
    background: #f5f7fa;
    color: #909399;
    font-size: 24px;
}

.cover-uploader {
    display: inline-block;
}

.cover-uploader :deep(.el-upload) {
    border: 1px dashed #d9d9d9;
    border-radius: 6px;
    cursor: pointer;
    position: relative;
    overflow: hidden;
    transition: all 0.3s;
}

.cover-uploader :deep(.el-upload:hover) {
    border-color: #409eff;
}

.cover-uploader-icon {
    font-size: 28px;
    color: #8c939d;
    width: 150px;
    height: 150px;
    text-align: center;
    line-height: 150px;
}

.cover-uploader-icon.uploading {
    animation: rotate 1s linear infinite;
}

@keyframes rotate {
    from {
        transform: rotate(0deg);
    }

    to {
        transform: rotate(360deg);
    }
}

.cover-image {
    width: 150px;
    height: 150px;
    display: block;
    object-fit: cover;
}

.batch-edit-container {
    min-height: 400px;
}

.batch-edit-toolbar {
    margin-bottom: 15px;
}

.image-slot-small {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 100%;
    height: 100%;
    background: #f5f7fa;
    color: #909399;
    font-size: 16px;
}
</style>
