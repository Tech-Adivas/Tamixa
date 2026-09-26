import "@testing-library/jest-dom";

// Mock URL.createObjectURL and URL.revokeObjectURL for audio tests
global.URL.createObjectURL = jest.fn(() => "mock-object-url");
global.URL.revokeObjectURL = jest.fn();

// Mock HTMLMediaElement methods for audio tests
window.HTMLMediaElement.prototype.pause = jest.fn();
window.HTMLMediaElement.prototype.play = jest.fn(() => Promise.resolve());
window.HTMLMediaElement.prototype.load = jest.fn();
