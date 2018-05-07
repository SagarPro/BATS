package sagsaguz.enquirytracking.utils

import android.content.Context
import com.amazonaws.auth.CognitoCachingCredentialsProvider
import com.amazonaws.regions.Regions

class AWSProvider {

    fun getCredentialsProvider(context: Context): CognitoCachingCredentialsProvider {
        return CognitoCachingCredentialsProvider(
                context,
                "us-east-1:d39564d1-84d9-4c1b-88b9-e76154e32e78",
                Regions.US_EAST_1
        )
    }

}