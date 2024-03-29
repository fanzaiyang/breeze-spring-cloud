package cn.fanzy.smart.flow.engine;

import cn.fanzy.smart.flow.model.entity.FlowTemplateInfoEntity;

/**
 * 流程引擎
 *
 * @author fanzaiyang
 * @date 2024/03/11
 */
public interface FlowEngine {

    /**
     * 句柄创建流模板
     *
     * @param flowTemplate 流程模板
     * @return {@link String}
     */
    String handleCreateFlowTemplate(FlowTemplateInfoEntity flowTemplate);

}
