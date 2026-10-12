// Chapter 98: start the agent, show its progress, then show the outcome.
// The demo identifies the customer with a header; a real site uses its login session instead (Chapter 84).
const CUSTOMER = { "X-Customer-Id": "C-1001" };
const form = document.getElementById("issue-form");
const steps = document.getElementById("steps");
const reply = document.getElementById("reply");
const cancel = document.getElementById("cancel");
let current = null;

form.addEventListener("submit", async (event) => {
    event.preventDefault();
    steps.replaceChildren();
    reply.textContent = "";
    const body = Object.fromEntries(new FormData(form));
    const started = await fetch("/api/issues/async", {
        method: "POST",
        headers: { ...CUSTOMER, "Content-Type": "application/json" },
        body: JSON.stringify(body)
    });
    if (!started.ok) {
        reply.textContent = "Please check the order number and your message.";
        return;
    }
    current = started.headers.get("Location");
    cancel.hidden = false;
    await followProgress(current);
    cancel.hidden = true;
    const outcome = await (await fetch(current, { headers: CUSTOMER })).json();
    // The reply was written by a model: always insert it as text, never as HTML.
    reply.textContent = outcome.reply ?? outcome.message;
});

cancel.addEventListener("click", () => fetch(current, { method: "DELETE", headers: CUSTOMER }));

// EventSource cannot send headers, so read the event stream with fetch.
async function followProgress(location) {
    const response = await fetch(location + "/events", { headers: { ...CUSTOMER, Accept: "text/event-stream" } });
    const reader = response.body.pipeThrough(new TextDecoderStream()).getReader();
    let buffer = "";
    for (;;) {
        const { value, done } = await reader.read();
        if (done) return;
        buffer += value;
        let end;
        while ((end = buffer.indexOf("\n\n")) >= 0) {
            const data = buffer.slice(0, end).split("\n").find(line => line.startsWith("data:"));
            buffer = buffer.slice(end + 2);
            if (!data || data === "data:done") continue;
            const item = document.createElement("li");
            item.textContent = data.slice(5);
            steps.append(item);
        }
    }
}
