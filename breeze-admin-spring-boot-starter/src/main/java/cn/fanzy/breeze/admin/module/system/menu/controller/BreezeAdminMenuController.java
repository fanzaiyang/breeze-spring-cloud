package cn.fanzy.breeze.admin.module.system.menu.controller;

import cn.fanzy.breeze.admin.module.entity.SysMenu;
import cn.fanzy.breeze.admin.module.system.menu.args.BreezeAdminMenuEnableArgs;
import cn.fanzy.breeze.admin.module.system.menu.args.BreezeAdminMenuQueryArgs;
import cn.fanzy.breeze.admin.module.system.menu.args.BreezeAdminMenuSaveArgs;
import cn.fanzy.breeze.admin.module.system.menu.service.BreezeAdminMenuService;
import cn.fanzy.breeze.web.model.JsonContent;
import cn.hutool.core.lang.tree.Tree;
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

@Api(tags = "「微风组件」菜单管理")
@ApiSupport(author = "微风组件", order = 992021)
@AllArgsConstructor
@RestController
@RequestMapping("${breeze.admin.prefix.api?:/${breeze.admin.prefix.menu?:/sys/menu}}")
public class BreezeAdminMenuController {
    private final BreezeAdminMenuService breezeAdminMenuService;
    @ApiOperation(value = "保存修改")
    @ApiOperationSupport(order = 1)
    @PostMapping("/save")
    public JsonContent<Object> save(@Valid @RequestBody BreezeAdminMenuSaveArgs args){
        return breezeAdminMenuService.save(args);
    }
    @ApiOperation(value = "删除单个")
    @ApiImplicitParam(name = "id",value = "菜单ID")
    @ApiOperationSupport(order = 5)
    @DeleteMapping("/delete")
    public JsonContent<Object> delete(String id){
        return breezeAdminMenuService.delete(id);
    }
    @ApiOperation(value = "删除批量")
    @ApiImplicitParam(name = "id",value = "菜单ID集合")
    @ApiOperationSupport(order = 10)
    @DeleteMapping("/delete/batch")
    public JsonContent<Object> delete(List<String> id){
        return breezeAdminMenuService.deleteBatch(id);
    }
    @ApiOperation(value = "启禁用单个")
    @ApiImplicitParam(name = "id",value = "菜单ID")
    @ApiOperationSupport(order = 15)
    @PostMapping("/enable")
    public JsonContent<Object> enable(String id){
        return breezeAdminMenuService.enable(id);
    }
    @ApiOperation(value = "启禁用批量")
    @ApiOperationSupport(order = 20)
    @PostMapping("/enable/batch")
    public JsonContent<Object> enable(@Valid @RequestBody BreezeAdminMenuEnableArgs args){
        return breezeAdminMenuService.enableBatch(args);
    }
    @ApiOperation(value = "分页查询")
    @ApiOperationSupport(order = 25)
    @PostMapping("/query/page")
    public JsonContent<Page<SysMenu>> query(@Valid @RequestBody BreezeAdminMenuQueryArgs args){
        return breezeAdminMenuService.queryPage(args);
    }
    @ApiOperation(value = "查询所有")
    @ApiOperationSupport(order = 30)
    @PostMapping("/query/all")
    public JsonContent<List<SysMenu>> queryAll(@Valid @RequestBody BreezeAdminMenuQueryArgs args){
        return breezeAdminMenuService.queryAll(args);
    }
    @ApiOperation(value = "查询树")
    @ApiOperationSupport(order = 35)
    @PostMapping("/query/tree")
    public JsonContent<List<Tree<String>>> queryTree(@Valid @RequestBody BreezeAdminMenuQueryArgs args){
        return breezeAdminMenuService.queryTree(args);
    }
}
