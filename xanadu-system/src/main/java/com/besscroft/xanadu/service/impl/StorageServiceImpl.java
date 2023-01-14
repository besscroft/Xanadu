package com.besscroft.xanadu.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.besscroft.xanadu.common.converter.StorageConverterMapper;
import com.besscroft.xanadu.common.entity.Storage;
import com.besscroft.xanadu.common.entity.StorageConfig;
import com.besscroft.xanadu.common.exception.XanaduException;
import com.besscroft.xanadu.common.param.storage.StorageAddParam;
import com.besscroft.xanadu.common.param.storage.StorageUpdateParam;
import com.besscroft.xanadu.common.vo.StorageInfoVo;
import com.besscroft.xanadu.mapper.StorageConfigMapper;
import com.besscroft.xanadu.mapper.StorageMapper;
import com.besscroft.xanadu.service.StorageConfigService;
import com.besscroft.xanadu.service.StorageService;
import com.github.pagehelper.PageHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.util.List;
import java.util.Objects;

/**
 * @Description
 * @Author Bess Croft
 * @Date 2022/12/18 21:13
 */
@Service
@RequiredArgsConstructor
public class StorageServiceImpl extends ServiceImpl<StorageMapper, Storage> implements StorageService {

    private final StorageConfigService storageConfigService;
    private final StorageConfigMapper storageConfigMapper;

    @Override
    public List<Storage> storagePage(Integer pageNum, Integer pageSize, Integer type) {
        PageHelper.startPage(pageNum, pageSize);
        return this.baseMapper.selectPage(type);
    }

    @Override
    public void deleteStorage(Long storageId) {
        Assert.isTrue(this.baseMapper.deleteById(storageId) > 0, "存储删除失败！");
        Assert.isTrue(storageConfigMapper.deleteByStorageId(storageId) > 0, "存储删除失败！");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addStorage(StorageAddParam param) {
        Storage storage = StorageConverterMapper.INSTANCE.AddParamToStorage(param);
        this.baseMapper.insert(storage);
        param.getConfigList().forEach(storageConfig -> storageConfig.setStorageId(storage.getId()));
        storageConfigService.saveBatch(param.getConfigList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStorage(StorageUpdateParam param) {
        Storage storage = StorageConverterMapper.INSTANCE.UpdateParamToStorage(param);
        Storage oldStorage = this.baseMapper.selectById(storage.getId());
        if (!Objects.equals(storage.getType(), oldStorage.getType()))
            throw new XanaduException("存储类型不允许修改！");
        this.baseMapper.updateById(storage);
        storageConfigService.updateBatchById(param.getConfigList());
    }

    @Override
    public StorageInfoVo getInfo(Long storageId) {
        Storage storage = this.baseMapper.selectById(storageId);
        StorageInfoVo vo = StorageConverterMapper.INSTANCE.StorageToInfoVo(storage);
        // 配置信息查询
        List<StorageConfig> configList = storageConfigMapper.selectByStorageId(storageId);
        vo.setConfigList(configList);
        return vo;
    }

    @Override
    public void updateStatus(Long storageId, Integer status) {
        Storage storage = this.baseMapper.selectById(storageId);
        Assert.notNull(storage, "存储不存在！");
        storage.setEnable(status);
        Assert.isTrue(this.baseMapper.updateById(storage) > 0, "更新状态失败！");
    }

}
