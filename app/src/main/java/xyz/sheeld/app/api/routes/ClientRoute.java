package xyz.sheeld.app.api.routes;


import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;
import xyz.sheeld.app.api.dtos.PostClientJoinRequestDTO;
import xyz.sheeld.app.api.dtos.PostClientJoinResponseDTO;

public interface ClientRoute {
    @POST("/client/join")
    Call<PostClientJoinResponseDTO> joinClient(
            @Body() PostClientJoinRequestDTO body
    );
}
