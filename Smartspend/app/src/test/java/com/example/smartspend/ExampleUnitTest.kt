package com.example.smartspend.utils

import com.example.smartspend.LoginValidator
import com.example.smartspend.Validator
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun validateEmail_isCorrect() {
        val validEmail = "test@example.com"
        val invalidEmail = "test@com"

        assertTrue(Validator.isValidEmail(validEmail))
        assertFalse(Validator.isValidEmail(invalidEmail))
    }

    @Test
    fun validatePassword_isCorrect() {
        val strongPassword = "Password123"
        val weakPasswordNoNumbers = "Password"
        val weakPasswordNoLetters = "12345678"
        val shortPassword = "Pass1"

        assertTrue(Validator.isValidPassword(strongPassword))
        assertFalse(Validator.isValidPassword(weakPasswordNoNumbers))
        assertFalse(Validator.isValidPassword(weakPasswordNoLetters))
        assertFalse(Validator.isValidPassword(shortPassword))
    }

    @Test
    fun testValidEmail() {
        val validEmail = "test@example.com"
        val invalidEmail = "test.com"

        assertTrue(LoginValidator.isValidEmail(validEmail))
        assertFalse(LoginValidator.isValidEmail(invalidEmail))
    }

    @Test
    fun testValidPassword() {
        val validPassword = "password123"
        val emptyPassword = ""

        assertTrue(LoginValidator.isValidPassword(validPassword))
        assertFalse(LoginValidator.isValidPassword(emptyPassword))
    }

    @Test
    fun testLoginUserMock_Success() {
        val email = "test@example.com"
        val password = "password123"

        val response = LoginValidator.loginUserMock(email, password)
        assertEquals(123, response["userID"])
        assertEquals("Login successful", response["message"])
    }

    @Test
    fun testLoginUserMock_Failure() {
        val email = "test@com"
        val password = "password123"

        val response = LoginValidator.loginUserMock(email, password)
        assertEquals("Login failed", response["message"])
    }

    @Test
    fun testSaveUserIDToPreferencesMock() {
        val validUserID = 123
        val invalidUserID = -1

        assertTrue(LoginValidator.saveUserIDToPreferencesMock(validUserID))
        assertFalse(LoginValidator.saveUserIDToPreferencesMock(invalidUserID))
    }
}
