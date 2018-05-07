package sagsaguz.enquirytracking.utils

import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBAttribute
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBHashKey
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBIndexHashKey
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBIndexRangeKey
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBRangeKey
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBTable
import java.io.Serializable

@DynamoDBTable(tableName = "enquirytracking-mobilehub-1860763478-Center")
class CenterDO : Serializable{
    @get:DynamoDBHashKey(attributeName = "phone")
    @get:DynamoDBAttribute(attributeName = "phone")
    var phone: String? = null
    @get:DynamoDBRangeKey(attributeName = "emailId")
    @get:DynamoDBAttribute(attributeName = "emailId")
    var emailId: String? = null
    @get:DynamoDBAttribute(attributeName = "location")
    var location: String? = null
    @get:DynamoDBAttribute(attributeName = "name")
    var name: String? = null
    @get:DynamoDBAttribute(attributeName = "password")
    var password: String? = null
    @get:DynamoDBAttribute(attributeName = "directions")
    var directions: String? = null
    @get:DynamoDBAttribute(attributeName = "accessType")
    var accessType: String? = null

    fun phone(_phone: String) {
        this.phone = _phone
    }

    fun emailId(_emailId: String) {
        this.emailId = _emailId
    }

    fun location(_location: String) {
        this.location = _location
    }

    fun name(_name: String) {
        this.name = _name
    }

    fun password(_password: String) {
        this.password = _password
    }

    fun directions(_directions: String) {
        this.directions = _directions
    }

    fun accessType(_accessType: String) {
        this.accessType = _accessType
    }

}
