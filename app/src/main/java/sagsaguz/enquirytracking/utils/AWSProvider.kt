package sagsaguz.enquirytracking.utils

import android.content.Context
import com.amazonaws.auth.CognitoCachingCredentialsProvider
import com.amazonaws.regions.Regions

class AWSProvider {

    fun getCredentialsProvider(context: Context): CognitoCachingCredentialsProvider {
        return CognitoCachingCredentialsProvider(
                context,
                "your_pool_id",
                Regions.US_EAST_1
        )
    }

}
