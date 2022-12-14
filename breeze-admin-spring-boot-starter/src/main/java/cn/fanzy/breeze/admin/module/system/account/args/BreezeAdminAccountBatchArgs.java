package cn.fanzy.breeze.admin.module.system.account.args;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import java.util.List;

/**
 * 微风del批arg游戏管理员帐户
 *
 * @author fanzaiyang
 * @since 2022-11-03
 */
@Data
public class BreezeAdminAccountBatchArgs {
    @NotEmpty(message = "请求参数不能为空！")
    @Schema(description = "账户ID集合")
    private List<String> idList;
}
