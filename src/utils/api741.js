/**
 * API utility functions for HTTP requests.
 * Wraps fetch/axios calls with error handling and auth.
 */

const API_BASE_URL = process.env.REACT_APP_API_URL || 'http://localhost:8080/api';

/**
 * Gets the authorization header with JWT token.
 * @returns {object} Headers object with Authorization
 */
const getAuthHeaders = () => {
    const token = localStorage.getItem('jwttoken');
    return {
        'Content-Type': 'application/json',
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
    };
};

/**
 * Makes a GET request to the API.
 * @param {string} endpoint - The API endpoint
 * @returns {Promise} Response data
 */
export const apiGet = async (endpoint) => {
    const response = await fetch(`${API_BASE_URL}${endpoint}`, {
        method: 'GET',
        headers: getAuthHeaders(),
    });
    if (!response.ok) {
        throw new Error(`API Error: ${response.status}`);
    }
    return response.json();
};

/**
 * Makes a POST request to the API.
 * @param {string} endpoint - The API endpoint
 * @param {object} data - The request body
 * @returns {Promise} Response data
 */
export const apiPost = async (endpoint, data) => {
    const response = await fetch(`${API_BASE_URL}${endpoint}`, {
        method: 'POST',
        headers: getAuthHeaders(),
        body: JSON.stringify(data),
    });
    if (!response.ok) {
        throw new Error(`API Error: ${response.status}`);
    }
    return response.json();
};

/**
 * Makes a PUT request to the API.
 * @param {string} endpoint - The API endpoint
 * @param {object} data - The request body
 * @returns {Promise} Response data
 */
export const apiPut = async (endpoint, data) => {
    const response = await fetch(`${API_BASE_URL}${endpoint}`, {
        method: 'PUT',
        headers: getAuthHeaders(),
        body: JSON.stringify(data),
    });
    if (!response.ok) {
        throw new Error(`API Error: ${response.status}`);
    }
    return response.json();
};

/**
 * Makes a DELETE request to the API.
 * @param {string} endpoint - The API endpoint
 * @returns {Promise} Response data
 */
export const apiDelete = async (endpoint) => {
    const response = await fetch(`${API_BASE_URL}${endpoint}`, {
        method: 'DELETE',
        headers: getAuthHeaders(),
    });
    if (!response.ok) {
        throw new Error(`API Error: ${response.status}`);
    }
    return response.json();
};

export default { apiGet, apiPost, apiPut, apiDelete };


/**
 * Debounce function to limit rapid invocations.
 * @param {Function} func - The function to debounce
 * @param {number} wait - Delay in milliseconds
 * @returns {Function} Debounced function
 */
const debounce = (func, wait = 300) => {
    let timeout;
    return (...args) => {
        clearTimeout(timeout);
        timeout = setTimeout(() => func.apply(this, args), wait);
    };
};

