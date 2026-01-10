package com.LogTriage.LogTriage;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

public class SchemaValidator {

    private final ObjectMapper mapper;

    public SchemaValidator() {
        this.mapper = new ObjectMapper();
    }

    /**
     * Validates a JSON file against the Schema.
     * @param jsonFilePath Path to the .json report file (e.g., "golden/npe.report.json")
     * @param schemaFilePath Path to the .schema.json file (e.g., "spec/triage-report.schema.json")
     * @return true if valid, false if invalid
     */
    public boolean validate(String jsonFilePath, String schemaFilePath) throws IOException {
        // 1. Load the Schema
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V201909);
        InputStream schemaStream = new FileInputStream(new File(schemaFilePath));
        JsonSchema schema = factory.getSchema(schemaStream);

        // 2. Load the Data
        JsonNode data = mapper.readTree(new File(jsonFilePath));

        // 3. Validate
        Set<ValidationMessage> errors = schema.validate(data);

        // 4. Report Results
        if (errors.isEmpty()) {
            System.out.println("✅ VALID: " + jsonFilePath);
            return true;
        } else {
            System.out.println("❌ INVALID: " + jsonFilePath);
            for (ValidationMessage error : errors) {
                System.out.println("   -> " + error.getMessage());
            }
            return false;
        }
    }
}