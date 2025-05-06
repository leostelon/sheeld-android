package xyz.sheeld.app.api.routes;


import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;
import xyz.sheeld.app.api.dtos.PostWalletOverviewResponseDTO;
import xyz.sheeld.app.api.dtos.PostWalletOverviewRequestDTO;

public interface WalletRoute {
    @POST("/wallet/balance")
    Call<PostWalletOverviewResponseDTO> getWalletOverview(
            @Body() PostWalletOverviewRequestDTO body
    );
}
