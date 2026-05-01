import { ref } from 'vue';

const eventBus = ref({});

export const emit = (event, data) => {
  if (eventBus.value[event]) {
    eventBus.value[event].forEach(callback => callback(data));
  }
};

export const on = (event, callback) => {
  if (!eventBus.value[event]) {
    eventBus.value[event] = [];
  }
  eventBus.value[event].push(callback);
};

export const off = (event, callback) => {
  if (eventBus.value[event]) {
    eventBus.value[event] = eventBus.value[event].filter(cb => cb !== callback);
  }
};