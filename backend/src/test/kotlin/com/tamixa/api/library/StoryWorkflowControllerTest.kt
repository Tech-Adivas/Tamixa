package com.tamixa.api.library

import com.tamixa.application.admin.AdminService
import com.tamixa.application.storylibrary.StoryLibraryService
import com.tamixa.domain.LibraryStory
import com.tamixa.domain.LibraryStoryStatus
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.mockito.Mockito.*
import org.junit.jupiter.api.BeforeEach
import org.mockito.Mock
import org.mockito.MockitoAnnotations

/**
 * Unit tests for StoryWorkflowController.
 * 
 * Tests the story management workflow API endpoints:
 * - Submit for review
 * - Approve content
 * - Request changes
 * - Reject story
 * - Publish story
 * - Unpublish story
 */
class StoryWorkflowControllerTest {
    
    @Mock
    private lateinit var storyLibraryService: StoryLibraryService
    
    @Mock
    private lateinit var adminService: AdminService
    
    private lateinit var controller: StoryWorkflowController
    
    @BeforeEach
    fun setup() {
        MockitoAnnotations.openMocks(this)
        controller = StoryWorkflowController(storyLibraryService, adminService)
    }
    
    @Test
    fun `submitForReview should return success when story is submitted`() {
        // Given
        val storyId = 1L
        `when`(storyLibraryService.submitForReview(storyId)).thenReturn(true)
        
        // When
        val response = controller.submitForReview(storyId)
        
        // Then
        assertEquals(200, response.statusCode.value())
        assertNotNull(response.body)
        assertTrue(response.body!!.success)
        assertEquals("SUBMITTED", response.body!!.status)
        assertEquals(storyId, response.body!!.storyId)
        
        verify(storyLibraryService).submitForReview(storyId)
    }
    
    @Test
    fun `submitForReview should return error when story cannot be submitted`() {
        // Given
        val storyId = 1L
        `when`(storyLibraryService.submitForReview(storyId)).thenReturn(false)
        
        // When
        val response = controller.submitForReview(storyId)
        
        // Then
        assertEquals(400, response.statusCode.value())
        assertNotNull(response.body)
        assertFalse(response.body!!.success)
        
        verify(storyLibraryService).submitForReview(storyId)
    }
    
    @Test
    fun `approveStory should return success when story is approved`() {
        // Given
        val storyId = 1L
        `when`(storyLibraryService.approveStoryContent(storyId)).thenReturn(true)
        
        // When
        val response = controller.approveStory(storyId)
        
        // Then
        assertEquals(200, response.statusCode.value())
        assertNotNull(response.body)
        assertTrue(response.body!!.success)
        assertEquals("APPROVED", response.body!!.status)
        assertEquals(storyId, response.body!!.storyId)
        
        verify(storyLibraryService).approveStoryContent(storyId)
    }
    
    @Test
    fun `requestChanges should return success with notes`() {
        // Given
        val storyId = 1L
        val notes = "Please fix the grammar"
        val request = com.tamixa.api.library.dto.StoryWorkflowRequest(notes)
        `when`(storyLibraryService.requestStoryChanges(storyId, notes)).thenReturn(true)
        
        // When
        val response = controller.requestChanges(storyId, request)
        
        // Then
        assertEquals(200, response.statusCode.value())
        assertNotNull(response.body)
        assertTrue(response.body!!.success)
        assertEquals("CHANGES_REQUESTED", response.body!!.status)
        
        verify(storyLibraryService).requestStoryChanges(storyId, notes)
    }
    
    @Test
    fun `rejectStory should return success with notes`() {
        // Given
        val storyId = 1L
        val notes = "Content not suitable"
        val request = com.tamixa.api.library.dto.StoryWorkflowRequest(notes)
        `when`(storyLibraryService.rejectStoryContent(storyId, notes)).thenReturn(true)
        
        // When
        val response = controller.rejectStory(storyId, request)
        
        // Then
        assertEquals(200, response.statusCode.value())
        assertNotNull(response.body)
        assertTrue(response.body!!.success)
        assertEquals("REJECTED", response.body!!.status)
        
        verify(storyLibraryService).rejectStoryContent(storyId, notes)
    }
    
    @Test
    fun `publishStory should return success when story is published`() {
        // Given
        val storyId = 1L
        `when`(storyLibraryService.publishStory(storyId)).thenReturn(true)
        
        // When
        val response = controller.publishStory(storyId)
        
        // Then
        assertEquals(200, response.statusCode.value())
        assertNotNull(response.body)
        assertTrue(response.body!!.success)
        assertEquals("PUBLISHED", response.body!!.status)
        
        verify(storyLibraryService).publishStory(storyId)
    }
    
    @Test
    fun `unpublishStory should return success when story is unpublished`() {
        // Given
        val storyId = 1L
        `when`(storyLibraryService.unpublishStory(storyId)).thenReturn(true)
        
        // When
        val response = controller.unpublishStory(storyId)
        
        // Then
        assertEquals(200, response.statusCode.value())
        assertNotNull(response.body)
        assertTrue(response.body!!.success)
        assertEquals("DRAFT", response.body!!.status)
        
        verify(storyLibraryService).unpublishStory(storyId)
    }
}
