package sagsaguz.enquirytracking.utils

import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBAttribute
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBHashKey
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBIndexHashKey
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBIndexRangeKey
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBRangeKey
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBTable
import com.amazonaws.services.dynamodbv2.model.AttributeValue
import java.io.Serializable

@DynamoDBTable(tableName = "enquirytracking-mobilehub-1860763478-CustomerDetails")
class CustomerDetailsDO : Serializable{
    @get:DynamoDBHashKey(attributeName = "phone")
    @get:DynamoDBAttribute(attributeName = "phone")
    var phone: String? = null
    @get:DynamoDBRangeKey(attributeName = "emailId")
    @get:DynamoDBAttribute(attributeName = "emailId")
    var emailId: String? = null
    @get:DynamoDBAttribute(attributeName = "NFD")
    var nfd: Map<String, String>? = null
    @get:DynamoDBAttribute(attributeName = "admissionFor")
    var admissionFor: String? = null
    @get:DynamoDBAttribute(attributeName = "childAge")
    var childAge: String? = null
    @get:DynamoDBAttribute(attributeName = "childName")
    var childName: String? = null
    @get:DynamoDBAttribute(attributeName = "createdDate")
    var createdDate: String? = null
    @get:DynamoDBAttribute(attributeName = "enquiryType")
    var enquiryType: String? = null
    @get:DynamoDBAttribute(attributeName = "hyk")
    var hyk: String? = null
    @get:DynamoDBAttribute(attributeName = "locality")
    var locality: String? = null
    @get:DynamoDBAttribute(attributeName = "parentName")
    var parentName: String? = null
    @get:DynamoDBAttribute(attributeName = "rating")
    var rating: String? = null
    @get:DynamoDBAttribute(attributeName = "center")
    var center: String? = null
    @get:DynamoDBAttribute(attributeName = "status")
    var status: String? = null
    @get:DynamoDBAttribute(attributeName = "leadStage")
    var leadStage: String? = null

    fun phone(_phone: String) {
        this.phone = _phone
    }

    fun emailId(_emailId: String) {
        this.emailId = _emailId
    }

    fun admissionFor(_admissionFor: String) {
        this.admissionFor = _admissionFor
    }

    fun childAge(_childAge: String) {
        this.childAge = _childAge
    }

    fun childName(_childName: String) {
        this.childName = _childName
    }

    fun createdDate(_createdDate: String) {
        this.createdDate = _createdDate
    }

    fun enquiryType(_enquiryType: String) {
        this.enquiryType = _enquiryType
    }

    fun hyk(_hyk: String) {
        this.hyk = _hyk
    }

    fun locality(_locality: String) {
        this.locality = _locality
    }

    fun parentName(_parentName: String) {
        this.parentName = _parentName
    }

    fun rating(_rating: String) {
        this.rating = _rating
    }

    fun nfd(_nfd: Map<String, String>){
        this.nfd = _nfd
    }

    fun center(_center: String) {
        this.center = _center
    }

    fun status(_status: String) {
        this.status = _status
    }

    fun leadStage(_leadStage: String) {
        this.leadStage = _leadStage
    }

}
