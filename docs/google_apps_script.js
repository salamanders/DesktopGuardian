// Desktop Guardian - Google Apps Script Receiver
// Copy this code into a Google Apps Script project attached to your Spreadsheet.
// 1. Open your Google Sheet.
// 2. Go to Extensions > Apps Script.
// 3. Paste this code into Code.gs.
// 4. Click Deploy > New Deployment.
// 5. Select type: Web App.
// 6. Set "Who has access" to "Anyone" (so the desktop app can post to it without OAuth complexity).
// 7. Copy the "Web App URL" and paste it into the Desktop Guardian app.

function doPost(e) {
  try {
    var sheet = SpreadsheetApp.getActiveSpreadsheet().getActiveSheet();
    var jsonData = JSON.parse(e.postData.contents);

    // Check if headers exist, if not, create them
    if (sheet.getLastRow() === 0) {
      sheet.appendRow(["Timestamp", "Date", "Type", "Severity", "Message", "Details"]);
    }

    // Extract data from the Alert object
    // Expected JSON structure:
    // {
    //   "type": "APP_ADDED",
    //   "severity": "INFO",
    //   "message": "New App: Discord",
    //   "details": "Version 1.0.0",
    //   "timestamp": 1715623423423
    // }

    var timestamp = jsonData.timestamp || Date.now();
    var dateString = new Date(timestamp).toLocaleString();

    sheet.appendRow([
      timestamp,
      dateString,
      jsonData.type || "UNKNOWN",
      jsonData.severity || "INFO",
      jsonData.message || "No message",
      jsonData.details || ""
    ]);

    return ContentService.createTextOutput(JSON.stringify({"status": "success"}))
      .setMimeType(ContentService.MimeType.JSON);

  } catch (error) {
    return ContentService.createTextOutput(JSON.stringify({"status": "error", "message": error.toString()}))
      .setMimeType(ContentService.MimeType.JSON);
  }
}
