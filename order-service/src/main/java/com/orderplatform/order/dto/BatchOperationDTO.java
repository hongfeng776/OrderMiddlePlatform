package com.orderplatform.order.dto;

import lombok.Data;
import java.util.List;

@Data
public class BatchOperationDTO {

    private List<String> orderNos;

    private String remark;

    private Long operatorId;

    private String operatorName;
}
