package com.prospecta.ai.provider;

import com.prospecta.ai.dto.AiDto.AiRequest;
import com.prospecta.ai.dto.AiDto.AiResponse;

public interface AiProvider {

    AiResponse generate(AiRequest request);

    String getProviderName();
}
