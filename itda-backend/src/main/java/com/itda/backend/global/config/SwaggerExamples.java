package com.itda.backend.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.responses.ApiResponse;

public final class SwaggerExamples {

    private SwaggerExamples() {
    }

    public static void registerResponses(Components components) {
        components
                .addResponses("ProjectCreateSuccess", exampleResponse("Create project success", """
                        {
                          "code": "SUCCESS",
                          "message": "생성 완료",
                          "data": {
                            "projectId": 101,
                            "title": "Mars Vlog",
                            "role": "OWNER",
                            "createdAt": "2026-01-20T07:11:53.397Z"
                          },
                          "details": null
                        }
                        """))
                .addResponses("ProjectListSuccess", exampleResponse("List projects success", """
                        {
                          "code": "SUCCESS",
                          "message": null,
                          "data": {
                            "items": [
                              {
                                "projectId": 101,
                                "title": "Mars Vlog",
                                "thumbnailUrl": "https://example.com/thumbnail.jpg",
                                "role": "OWNER",
                                "memberCount": 3,
                                "sceneCount": 5,
                                "updatedAt": "2026-01-20T07:11:53.397Z"
                              }
                            ],
                            "page": 0,
                            "size": 20,
                            "total": 1
                          },
                          "details": null
                        }
                        """))
                .addResponses("ProjectDetailSuccess", exampleResponse("Get project detail success", """
                        {
                          "code": "SUCCESS",
                          "message": null,
                          "data": {
                            "projectId": 101,
                            "title": "Mars Vlog",
                            "description": "A story on Mars",
                            "genre": "SF",
                            "myRole": "OWNER",
                            "ownerId": 1,
                            "members": [
                              {
                                "userId": 1,
                                "email": "owner@test.com",
                                "name": "Owner",
                                "role": "OWNER",
                                "profileImage": "https://example.com/profile.jpg"
                              },
                              {
                                "userId": 2,
                                "name": "Kim",
                                "role": "EDITOR"
                              }
                            ],
                            "createdAt": "2026-01-20T07:11:53.397Z"
                          },
                          "details": null
                        }
                        """))
                .addResponses("SceneCreateSuccess", exampleResponse("Create scene success", """
                        {
                          "code": "SUCCESS",
                          "message": "생성 완료",
                          "data": {
                            "sceneId": 201,
                            "title": "Scene 1",
                            "order": 1
                          },
                          "details": null
                        }
                        """))
                .addResponses("SceneListSuccess", exampleResponse("List scenes success", """
                        {
                          "code": "SUCCESS",
                          "message": null,
                          "data": [
                            {
                              "sceneId": 201,
                              "title": "Scene 1",
                              "order": 1,
                              "status": "COMPLETED"
                            },
                            {
                              "sceneId": 202,
                              "title": "Scene 2",
                              "order": 2
                            }
                          ],
                          "details": null
                        }
                        """))
                .addResponses("SceneDetailSuccess", exampleResponse("Get scene detail success", """
                        {
                          "code": "SUCCESS",
                          "message": null,
                          "data": {
                            "sceneId": 201,
                            "projectId": 101,
                            "title": "Scene 1",
                            "description": "Morning at the base",
                            "order": 1
                          },
                          "details": null
                        }
                        """))
                .addResponses("Unauthorized", exampleResponse("Unauthorized", """
                        {
                          "code": "UNAUTHORIZED",
                          "message": "Unauthorized",
                          "data": null,
                          "details": null
                        }
                        """))
                .addResponses("Forbidden", exampleResponse("Forbidden", """
                        {
                          "code": "FORBIDDEN",
                          "message": "Forbidden",
                          "data": null,
                          "details": null
                        }
                        """))
                .addResponses("InvalidRequest", exampleResponse("Invalid request", """
                        {
                          "code": "INVALID_REQUEST",
                          "message": "Invalid request",
                          "data": null,
                          "details": null
                        }
                        """))
                .addResponses("ValidationError", exampleResponse("Validation error", """
                        {
                          "code": "INVALID_INPUT_VALUE",
                          "message": "Validation failed",
                          "data": null,
                          "details": {
                            "field": "error message"
                          }
                        }
                        """))
                .addResponses("ProjectNotFound", exampleResponse("Project not found", """
                        {
                          "code": "PROJECT_NOT_FOUND",
                          "message": "Project not found",
                          "data": null,
                          "details": null
                        }
                        """))
                .addResponses("SceneNotFound", exampleResponse("Scene not found", """
                        {
                          "code": "SCENE_NOT_FOUND",
                          "message": "Scene not found",
                          "data": null,
                          "details": null
                        }
                        """));
    }

    private static ApiResponse exampleResponse(String description, String exampleJson) {
        return new ApiResponse()
                .description(description)
                .content(new Content().addMediaType(
                        "application/json",
                        new MediaType().example(exampleJson)
                ));
    }
}
