package cn.fanzy.breeze.admin.module.system.roles.args;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotEmpty;
import java.util.List;

/**
 * 微风admin角色菜单绑定参数
 *
 * @author fanzaiyang
 * @date 2022-11-03
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BreezeAdminRoleMenuBindArgs {
    @NotEmpty(message = "角色ID不能为空！")
    @ApiModelProperty(value = "角色ID")
    private String id;
    @ApiModelProperty(value = "菜单ID集合")
    private List<String> menuIdList;
}
