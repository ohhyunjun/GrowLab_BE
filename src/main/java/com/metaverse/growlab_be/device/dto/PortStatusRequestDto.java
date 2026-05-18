package com.metaverse.growlab_be.device.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PortStatusRequestDto {

    private Integer portIndex;

    private Boolean status;
}
