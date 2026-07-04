-- Dev-mode seed data (FEAT-002): loaded by Quarkus in %dev only (%test sets sql-load-script=no-file).
-- The app_user oidc_subject values match the Keycloak ids pinned in dev-realm.json, so signing in
-- as ada/grace reuses these rows (UserProvisioningService finds them by subject) — the seeded APIs
-- and Ada's jump-back-in pointer belong to the users you can actually log in as.

INSERT INTO app_user (id, oidc_subject, display_name, email, created_at, updated_at, version) VALUES
    ('aaaaaaaa-0000-4000-8000-000000000001', '11111111-1111-4111-8111-111111111111', 'Ada Lovelace', 'ada@example.com', now(), now(), 0),
    ('aaaaaaaa-0000-4000-8000-000000000002', '22222222-2222-4222-8222-222222222222', 'Grace Hopper', 'grace@example.com', now(), now(), 0);

INSERT INTO spec (id, owner_id, title, description, api_version, resource_count, operation_count, body, created_at, updated_at, version) VALUES
    ('bbbbbbbb-0000-4000-8000-000000000001', 'aaaaaaaa-0000-4000-8000-000000000001', 'Storefront API', 'Sell products online.', '1.0', 5, 21, NULL, now() - interval '30 days', now() - interval '2 hours', 0),
    ('bbbbbbbb-0000-4000-8000-000000000002', 'aaaaaaaa-0000-4000-8000-000000000002', 'Billing API', 'Invoices, payments & refunds.', '2.3', 4, 18, NULL, now() - interval '90 days', now() - interval '3 days', 0),
    ('bbbbbbbb-0000-4000-8000-000000000003', 'aaaaaaaa-0000-4000-8000-000000000001', 'Fleet API', 'Vehicles, trips & drivers.', '0.4', 3, 12, NULL, now() - interval '20 days', now() - interval '1 week', 0),
    ('bbbbbbbb-0000-4000-8000-000000000004', 'aaaaaaaa-0000-4000-8000-000000000002', 'Notifications API', 'Email, SMS & push messages.', '0.9', 2, 8, NULL, now() - interval '10 days', now() - interval '5 days', 0);

INSERT INTO last_edited_location (id, user_id, spec_id, capability_name, created_at, updated_at, version) VALUES
    ('cccccccc-0000-4000-8000-000000000001', 'aaaaaaaa-0000-4000-8000-000000000001', 'bbbbbbbb-0000-4000-8000-000000000001', 'Add a product', now() - interval '2 hours', now() - interval '2 hours', 0);
