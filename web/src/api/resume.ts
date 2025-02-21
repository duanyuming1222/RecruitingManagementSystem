// 权限问题后期增加
import { get, post } from '/@/utils/http/axios';
import { UserState } from '/@/store/modules/user/types';
// import axios from 'axios';
enum URL {
    list = '/api/resume/list',
    detail = '/api/resume/detail',
    create = '/api/resume/create',
    update = '/api/resume/update',
    delete = '/api/resume/delete',
    preview = '/api/resume/file',
    download = '/api/resume/download'
}

const detailApi = async (params: any) => get<any>({ url: URL.detail, params, data: {}, headers: {} });

const listApi = async (params: any) => get<any>({ url: URL.list, params, data: {}, headers: {} });
const createApi = async (data: any) =>
    post<any>({ url: URL.create, params: {}, data, headers: { 'Content-Type': 'multipart/form-data;charset=utf-8' } });
const updateApi = async (data: any) =>
    post<any>({ url: URL.update, params: {}, data, headers: { 'Content-Type': 'multipart/form-data;charset=utf-8' } });
const deleteApi = async (params: any) => post<any>({ url: URL.delete, params, headers: {} });

// 获取文件预览URL
const getPreviewUrl = (filename: string) => `${URL.preview}/${filename}`;

// 获取文件下载URL
const getDownloadUrl = (filename: string) => `${URL.download}/${filename}`;

export { listApi, createApi, updateApi, deleteApi , detailApi, getPreviewUrl, getDownloadUrl};
