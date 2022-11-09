package cn.fanzy.breeze.admin.module.system.attachments.controller;

import cn.fanzy.breeze.admin.module.entity.SysFile;
import cn.fanzy.breeze.admin.module.system.attachments.args.BreezeAdminAttachmentBatchArgs;
import cn.fanzy.breeze.admin.module.system.attachments.args.BreezeAdminAttachmentQueryArgs;
import cn.fanzy.breeze.admin.module.system.attachments.service.BreezeAdminAttachmentService;
import cn.fanzy.breeze.admin.module.system.attachments.vo.TinyMCEVo;
import cn.fanzy.breeze.minio.config.BreezeMinioConfiguration;
import cn.fanzy.breeze.sqltoy.plus.conditions.toolkit.StringPool;
import cn.fanzy.breeze.web.model.JsonContent;
import cn.hutool.core.lang.Assert;
import com.github.xiaoymin.knife4j.annotations.ApiOperationSupport;
import com.github.xiaoymin.knife4j.annotations.ApiSupport;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.AllArgsConstructor;
import org.sagacity.sqltoy.model.Page;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.List;

/**
 * 微风管理附件控制器
 *
 * @author fanzaiyang
 * @date 2022-11-01
 */
@Api(tags = "「微风组件」附件管理")
@ApiSupport(author = "微风组件", order = 993001)
@AllArgsConstructor
@ConditionalOnClass(BreezeMinioConfiguration.class)
@RestController
@RequestMapping("${breeze.admin.prefix.api?:/${breeze.admin.prefix.account?:/sys/attachment}}")
public class BreezeAdminAttachmentController {
    private final BreezeAdminAttachmentService breezeAdminAttachmentService;

    @ApiOperation(value = "上传",notes = "支持批量上传，无需指定文件名。")
    @ApiOperationSupport(order = 1)
    @PostMapping("/upload")
    public JsonContent<List<SysFile>> upload(String prefix, HttpServletRequest request) {
        return breezeAdminAttachmentService.upload(prefix, request);
    }
    @ApiOperation(value = "上传TinyMCE",notes = "支持TinyMCE的上传。")
    @ApiOperationSupport(order = 1)
    @PostMapping("/upload/tiny")
    public TinyMCEVo uploadTinyMCE(String prefix, HttpServletRequest request) {
        JsonContent<List<SysFile>> upload = upload(prefix, request);
        if(!upload.isSuccess()){
            throw new RuntimeException(upload.getMessage());
        }
        String previewUrl = upload.getData().get(0).getPreviewUrl();
        Assert.notBlank(previewUrl,"预览地址不能为空！");
        return new TinyMCEVo(previewUrl.split(StringPool.QUESTION_MARK)[0]);
    }
    @ApiOperation(value = "获取单个")
    @ApiOperationSupport(order = 2)
    @GetMapping("/get")
    public JsonContent<SysFile> getFileInfo(String id) {
        return breezeAdminAttachmentService.getFileInfo(id);
    }

    @ApiOperation(value = "分页查询")
    @ApiOperationSupport(order = 3)
    @PostMapping("/query")
    public JsonContent<Page<SysFile>> queryPage(@Valid @RequestBody BreezeAdminAttachmentQueryArgs args){
        return breezeAdminAttachmentService.queryPage(args);
    }
    @ApiOperation(value = "删除")
    @ApiOperationSupport(order = 4)
    @PostMapping("/delete")
    public JsonContent<Object> delete(@Valid @RequestBody BreezeAdminAttachmentBatchArgs args){
        return breezeAdminAttachmentService.delete(args);
    }
}
