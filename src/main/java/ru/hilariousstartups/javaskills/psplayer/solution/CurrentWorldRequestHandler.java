package ru.hilariousstartups.javaskills.psplayer.solution;

import ru.hilariousstartups.javaskills.psplayer.swagger_codegen.model.CurrentTickRequest;
import ru.hilariousstartups.javaskills.psplayer.swagger_codegen.model.CurrentWorldResponse;

public interface CurrentWorldRequestHandler {
    void handle(CurrentWorldResponse currentWorldResponse, CurrentTickRequest request);
}
