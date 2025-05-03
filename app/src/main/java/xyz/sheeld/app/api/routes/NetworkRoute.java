package xyz.sheeld.app.api.routes;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import xyz.sheeld.app.api.dtos.GetNearestNodeResponseDTO;

public interface NetworkRoute {
    @GET("/network/nearest-support-node")
    Call<GetNearestNodeResponseDTO> getNearestNode();

    @GET("/network/all-nodes")
    Call<List<GetNearestNodeResponseDTO>> getNodes();
}
