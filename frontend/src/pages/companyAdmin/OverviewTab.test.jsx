import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import '@testing-library/jest-dom';
import OverviewTab from './OverviewTab';
import api from '../../services/api';

// Mock the api module
jest.mock('../../services/api');

describe('OverviewTab Premium Feature', () => {
  beforeEach(() => {
    // Clear all mocks before each test
    jest.clearAllMocks();
  });

  test('renders Upgrade to Premium button for non-premium users', async () => {
    // Mock localStorage for non-premium user
    Storage.prototype.getItem = jest.fn(() => JSON.stringify({ companyId: 1, subscriptionTier: 'FREE' }));

    // Mock the API responses
    api.get.mockImplementation((url) => {
      if (url === '/company-admin/employees') return Promise.resolve({ data: [] });
      if (url === '/company-admin/courses') return Promise.resolve({ data: [] });
      if (url === '/company-admin/enrollments') return Promise.resolve({ data: [] });
      if (url === '/company-admin/analytics/roi') return Promise.resolve({ data: null });
      return Promise.resolve({ data: {} });
    });

    render(<OverviewTab />);

    // Wait for data to load
    await waitFor(() => {
      expect(screen.queryByText(/Loading dashboard overview/i)).not.toBeInTheDocument();
    });

    // Check if Upgrade to Premium button is present
    const upgradeButton = screen.getByRole('button', { name: /Upgrade to Premium/i });
    expect(upgradeButton).toBeInTheDocument();
  });

  test('opens PremiumSubscriptionModal when Upgrade button is clicked', async () => {
    // Mock localStorage for non-premium user
    Storage.prototype.getItem = jest.fn(() => JSON.stringify({ companyId: 1, subscriptionTier: 'FREE' }));

    // Mock the API responses
    api.get.mockImplementation((url) => {
      return Promise.resolve({ data: [] });
    });

    render(<OverviewTab />);

    await waitFor(() => {
      expect(screen.queryByText(/Loading dashboard overview/i)).not.toBeInTheDocument();
    });

    const upgradeButton = screen.getByRole('button', { name: /Upgrade to Premium/i });
    fireEvent.click(upgradeButton);

    // Modal should now be visible
    expect(screen.getByText(/Pay ₹50,000/i)).toBeInTheDocument();
  });
});
