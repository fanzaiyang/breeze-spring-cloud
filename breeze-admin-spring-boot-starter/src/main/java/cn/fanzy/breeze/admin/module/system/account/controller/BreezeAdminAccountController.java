package cn.fanzy.breeze.admin.module.system.account.controller;

import cn.fanzy.breeze.admin.module.entity.SysAccount;
import cn.fanzy.breeze.admin.module.system.account.args.*;
import cn.fanzy.breeze.admin.module.system.account.service.BreezeAdminAccountService;
import cn.fanzy.breeze.web.model.JsonContent;
import com.github.xiaoymin.knife4j.annotations.ApiOperationSupport;
import com.github.xiaoymin.knife4j.annotations.ApiSupport;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import lombok.AllArgsConstructor;
import org.sagacity.sqltoy.model.Page;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * 微风管理员帐户控制器
 *
 * @author fanzaiyang
 * @date 2022-11-03
 */
@Api(tags = "「微风组件」账户管理")
@ApiSupport(author = "微风组件", order = 992001)
@AllArgsConstructor
@RestController
@RequestMapping("${breeze.admin.prefix.api?:/${breeze.admin.prefix.account?:/sys/account}}")
public class BreezeAdminAccountController {

    private final BreezeAdminAccountService breezeAdminAccountService;

    @ApiOperation(value = "新增修改")
    @ApiOperationSupport(order = 1)
    @PostMapping("/save")
    public JsonContent<Object> save(@Valid @RequestBody BreezeAdminAccountSaveArgs args) {
        return breezeAdminAccountService.save(args);
    }

    @ApiOperation(value = "绑定角色")
    @ApiOperationSupport(order = 1)
    @PostMapping("/role/bind")
    public JsonContent<Object> saveAccountRole(@Valid @RequestBody BreezeAdminAccountRoleSaveArgs args) {
        return breezeAdminAccountService.saveAccountRole(args);
    }

    @ApiOperation(value = "删除单个")
    @ApiOperationSupport(order = 2)
    @ApiImplicitParam(name = "id", value = "账户ID")
    @DeleteMapping("/delete")
    public JsonContent<Object> delete(String id) {
        return breezeAdminAccountService.delete(id);
    }

    @ApiOperation(value = "删除批量")
    @ApiOperationSupport(order = 3)
    @ApiImplicitParam(name = "idList", value = "账户ID集合['a','b']")
    @PostMapping("/delete/batch")
    public JsonContent<Object> deleteBatch(@Valid @RequestBody BreezeAdminAccountBatchArgs args) {
        return breezeAdminAccountService.deleteBatch(args.getIdList());
    }
    @ApiOperation(value = "启用批量")
    @ApiOperationSupport(order = 3)
    @ApiImplicitParam(name = "idList", value = "账户ID集合['a','b']")
    @PostMapping("/enable/batch")
    public JsonContent<Object> enableBatch(@Valid @RequestBody BreezeAdminAccountBatchArgs args) {
        return breezeAdminAccountService.enableBatch(args.getIdList());
    }
    @ApiOperation(value = "分页查询")
    @ApiOperationSupport(order = 4)
    @PostMapping("/query/page")
    public JsonContent<Page<SysAccount>> query(@Valid @RequestBody BreezeAdminAccountQueryArgs args) {
        return breezeAdminAccountService.query(args);
    }

    @ApiOperation(value = "查询账号绑定的角色")
    @ApiOperationSupport(order = 5)
    @GetMapping("/role/list")
    public JsonContent<List<String>> queryAccountRoleList(String id) {
        return breezeAdminAccountService.queryAccountRoleList(id);
    }
    @ApiOperation(value = "重置密码")
    @ApiOperationSupport(order = 6)
    @PostMapping("/pwd/reset")
    public JsonContent<Object> doRestAccountPwd(@Valid @RequestBody BreezeAdminAccountBatchArgs args) {
        return breezeAdminAccountService.doRestAccountPwd(args);
    }

    @ApiOperation(value = "修改密码")
    @ApiOperationSupport(order = 7)
    @PostMapping("/pwd/update")
    public JsonContent<Object> doChangeAccountPwd(@Valid @RequestBody BreezeAdminAccountPwdChangeArgs args) {
        return breezeAdminAccountService.doChangeAccountPwd(args);
    }
}
